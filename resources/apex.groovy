def dockerInit(){
    sh """
        systemctl status docker | grep 'Active:'
        sudo /usr/bin/pkill -f docker
        sudo /bin/systemctl restart docker
        docker system prune -a -f
        systemctl status docker | grep 'Active:'
    """
}

def call() {
    dockerInit()
    def gitRepo = "https://github.com/ROCmSoftwarePlatform/apex"
    def creds = "ROCm-Apps-Test from 2020-01-06"
    def docker_image = "rocm/pytorch:latest"
    def script = """
                docker run 
                pip uninstall -y apex
                pip install ninja
                cd /apex && python setup.py install  --cpp_ext --cuda_ext
                cd /apex/tests/L0/ && bash run_rocm.sh
                cd /apex/tests/distributed/ && bash run_rocm_distributed.sh
                cd /apex/apex/contrib/test/ && python run_rocm_extensions.py
            """

    checkout([$class: 'GitSCM',
                    userRemoteConfigs: [[url: gitRepo, credentialsId: creds]],
                    branches: [[name: "master"]]
                ]
            )
    dir("build"){
        node("gfx906"){
            docker.image('rocm/pytorch:latest').withRun('-it --detach --network=host --device=/dev/kfd --device=/dev/dri --ipc=host --shm-size 16G  --group-add video --cap-add=SYS_PTRACE --security-opt seccomp=unconfined -v `pwd`:/apex --name apex-rocm ${docker_image}') { c ->
                sh(script:script)
            }
        }
    }

}