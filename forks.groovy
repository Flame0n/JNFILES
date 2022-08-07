def setUpDocker(){
    println("Set up docker")
    // Example with simply shell script
    sh """
        systemctl status docker | grep 'Active:'
        sudo /usr/bin/pkill -f docker
        sudo /bin/systemctl restart docker
        docker system prune -a -f
        systemctl status docker | grep 'Active:'
    """
}

def executeCommand(String parameter) {
    println("Build executed")
    sh """
        build/tensorflow/tools/ci_build/ci_build.sh ROCM ./build/tensorflow/tools/ci_build/linux/rocm/run_${parameter}.sh
    """
}

def call() {
    node("rocm"){
        stage("Set up Docker"){
            setUpDocker()
        }
        stage("Clone project"){
            checkout(
                [
                    $class: 'GitSCM',
                    userRemoteConfigs: [[url: "https://github.com/ROCmSoftwarePlatform/tensorflow-upstream/", credentialsId:'Token']],
                    branches: [[name: "master"]],
                    extensions: [
                        [
                            $class: "RelativeTargetDirectory",
                            relativeTargetDir: "build"
                        ]
                    ],
                ]
            )
        }
        stage("Build execution"){
            executeCommand(params.executionType)
        }
    }
}
