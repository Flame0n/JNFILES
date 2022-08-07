def executeCommand(String parameter) {
    println("Build executed")
    sh """
        build/tensorflow/tools/ci_build/ci_build.sh ROCM ./build/tensorflow/tools/ci_build/linux/rocm/run_${parameter}.sh
    """
}

def call() {
    println("wooooooh")
    return 0

    node("rocm"){
        stage("Set up Docker"){
            restartDocker()
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
