def executeCommand(String parameter) {
    println("Build executed")
    sh """
        build/tensorflow/tools/ci_build/ci_build.sh ROCM ./build/tensorflow/tools/ci_build/linux/rocm/run_${parameter}.sh
    """
}

def executeStages(String repo="https://github.com/ROCmSoftwarePlatform/tensorflow-upstream/", String branch="master", String credentialsId="Token", String executionType="cpu"){
    node("rocm"){
        stage("Set up Docker"){
            restartDocker()
        }
        stage("Clone project"){
            checkout(
                [
                    $class: 'GitSCM',
                    userRemoteConfigs: [[url: repo, credentialsId:credentialsId]],
                    branches: [[name: branch]],
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

            executeCommand(executionType)
        }
    }
}

def call(Map parameters) {
    println(params.executionType)
    executeStages(String repo="https://github.com/ROCmSoftwarePlatform/tensorflow-upstream/", String branch="master", String credentialsId="Token", String executionType="cpu")
}
