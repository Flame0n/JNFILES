def CONGIF_MAP = [
    nightly : [
        stage: "Nightly",
        node: "rocm",
        script: """
            pwd
            mkdir -p wheels

            docker_image=rocm/pytorch:latest

            #docker build . -f Dockerfile -t \$docker_image
            docker run -it --detach --network=host --device=/dev/kfd --device=/dev/dri --ipc=host --shm-size 16G  --group-add video --cap-add=SYS_PTRACE --security-opt seccomp=unconfined -v `pwd`:/apex --name apex-rocm-nightly \$docker_image
            docker exec apex-rocm-nightly bash -c "pip uninstall -y apex"
            docker exec apex-rocm-nightly bash -c "pip install ninja"

            ### Install Apex ###
            docker exec apex-rocm-nightly bash -c "cd /apex && python setup.py install --cpp_ext --cuda_ext bdist_wheel 2>&1 |  tee apex_build.log"

            ### Single-GPU unit tests ###
            docker exec apex-rocm-nightly bash -c "cd /apex/tests/L0/ && bash run_rocm.sh"

            ### Multi-GPU unit tests ###
            docker exec apex-rocm-nightly bash -c "cd /apex/tests/distributed/ && bash run_rocm_distributed.sh"

            ### WHEEL ###
            docker exec apex-rocm-nightly bash -c "cd /apex; cp -a dist/*whl wheels/" 
        """
    ],

    master : [
        stage: "Master",
        node: "gfx906",
        script: """
            docker_image=rocm/pytorch:latest

            #docker build . -f Dockerfile -t \$docker_image
            docker run -it --detach --network=host --device=/dev/kfd --device=/dev/dri --ipc=host --shm-size 16G  --group-add video --cap-add=SYS_PTRACE --security-opt seccomp=unconfined -v `pwd`:/apex --name apex-rocm \$docker_image
            # Re-install PyTorch from the tip of ROCm fork
            docker exec apex-rocm bash -c "pip uninstall -y torch"
            docker exec apex-rocm bash -c "cd /var/lib/jenkins && rm -R pytorch && git clone https://github.com/ROCmSoftwarePlatform/pytorch.git"
            docker exec apex-rocm bash -c "cd /var/lib/jenkins/pytorch && git submodule sync && git submodule update --init --recursive && .jenkins/pytorch/build.sh 2>&1 |& tee build_log"

            # Re-install Apex
            docker exec apex-rocm bash -c "pip uninstall -y apex"
            docker exec apex-rocm bash -c "pip install ninja"
            docker exec apex-rocm bash -c "cd /apex && python setup.py install  --cpp_ext --cuda_ext"

            # Run Apex unit tests
            docker exec apex-rocm bash -c "cd /apex/tests/L0/ && bash run_rocm.sh"
            docker exec apex-rocm bash -c "cd /apex/tests/distributed/ && bash run_rocm_distributed.sh"
            docker exec apex-rocm bash -c "cd /apex/tests/distributed/ && bash run_rocm_distributed.sh"

            # Run Apex extension unit tests
            docker exec apex-rocm bash -c "cd /apex/apex/contrib/test/ && python run_rocm_extensions.py"
        """
    ],

    release: [
        stage: "Release",
        node: "gfx906", 
        script: """
            docker_image=rocm/pytorch:latest

            #docker build . -f Dockerfile -t \$docker_image
            docker run -it --detach --network=host --device=/dev/kfd --device=/dev/dri --ipc=host --shm-size 16G  --group-add video --cap-add=SYS_PTRACE --security-opt seccomp=unconfined -v `pwd`:/apex --name apex-rocm \$docker_image
            docker exec apex-rocm bash -c "pip uninstall -y apex"
            docker exec apex-rocm bash -c "pip install ninja"
            docker exec apex-rocm bash -c "cd /apex && python setup.py install  --cpp_ext --cuda_ext"
            docker exec apex-rocm bash -c "cd /apex/tests/L0/ && bash run_rocm.sh"
            docker exec apex-rocm bash -c "cd /apex/tests/distributed/ && bash run_rocm_distributed.sh"

            # Run Apex extension unit tests
            docker exec apex-rocm bash -c "cd /apex/apex/contrib/test/ && python run_rocm_extensions.py"
        """
    ]
]

def executeCommand(String customScript) {
    sh customScript
}

def restartDocker(){
    sh """
        systemctl status docker | grep 'Active:'
        sudo /usr/bin/pkill -f docker
        sudo /bin/systemctl restart docker
        docker system prune -a -f
        systemctl status docker | grep 'Active:'
    """
}

def checkoutProject(String repo, String branch){
    checkout(
        [
            $class: 'GitSCM',
            userRemoteConfigs: [[url: repo]],
            branches: [[name: branch]]
        ]
    )
}

def branch
def repository
def config

if (!env.GIT_URL && !env.GIT_BRANCH){
    branch = "master"
    repository = "https://github.com/ROCmSoftwarePlatform/apex"
    config = CONGIF_MAP.nightly
} else {
    branch = env.GIT_BRANCH
    repository = env.GIT_URL
    config = env.JOB_NAME.contains("master") ? CONGIF_MAP.master : CONGIF_MAP.release
}

pipeline {
    agent {
        node { label "${config.node ?: "none"}" }
    }
    options {
        timeout(time: 2, unit: 'HOURS')
        timestamps()
    }
    stages {
        stage("Nightly") {   
            steps {
                script {
                    restartDocker()
                    checkoutProject(repository, branch)
                    executeCommand(config.script)
                }
            }
        }
    }
    post {
        always {
            archiveArtifacts artifacts: "*.log", allowEmptyArchive: true
            archiveArtifacts artifacts: "wheels/*.whl", allowEmptyArchive: true
            emailext to: "lexainb3@gmail.com",
                subject: "jenkins build:${currentBuild.currentResult}: ${env.JOB_NAME}",
                body: "${currentBuild.currentResult}: Job ${env.JOB_NAME}\nMore Info can be found here: ${env.BUILD_URL}"
        }
    }
}