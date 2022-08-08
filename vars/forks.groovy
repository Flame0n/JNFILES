def executeCommand(String parameter) {
    println("Build executed")
    sh """
        tensorflow/tools/ci_build/ci_build.sh ROCM ./tensorflow/tools/ci_build/linux/rocm/run_${parameter}.sh
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
                    branches: [[name: branch]]
                ]
            )
        }
        stage("Build execution"){
            executeCommand(executionType)
        }
    }
}

def call(Map parameters) {
    try {
        
        executeStages(parameters["repo"], parameters["branch"], parameters["credentials_id"], parameters["executionType"])
        currentBuild.result = "SUCCESS"
        currentBuild.description = "<b>Success</b><br/>"
    } catch(e) {
        currentBuild.result = "FAILURE"
        currentBuild.description = "<b>Failed</b> when docker was executed<br/>"
    }
}
