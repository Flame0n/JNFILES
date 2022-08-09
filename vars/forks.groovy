def executeCommand(String executionType) {
    println("Build executed")
    sh """
        tensorflow/tools/ci_build/ci_build.sh ROCM ./tensorflow/tools/ci_build/linux/rocm/run_${executionType}.sh
    """
}

def executeStages(String repo="https://github.com/ROCmSoftwarePlatform/tensorflow-upstream/", String credentialsId="Token"){
    Map stages = [:]
    List listOfStages = ["run_cpu", "run_gpu_multi", "run_gpu_single", "rocm_ci_sanity"]
    String branch="develop-upstream"
    if (env.GIT_BRANCH && env.GIT_URL) {
        branch = env.CHANGE_BRANCH
        repo = env.GIT_URL
    }
    listOfStages.each() {
        stages[it] = {
            def label = it == "run_gpu_multi" ? "rocm&&multi_gpu" : "rocm"
            node(label){
                restartDocker()
                checkout(
                    [
                        $class: 'GitSCM',
                        userRemoteConfigs: [[url: repo]],
                        branches: [[name: branch]]
                    ]
                )
                executeCommand(executionType)
            }
        }
    }

    parallel stages
}

def call(Map parameters) {
    try {
        executeStages()
        currentBuild.result = "SUCCESS"
        currentBuild.description = "<b>Success</b><br/>"
    } catch(e) {
        currentBuild.result = "FAILURE"
        currentBuild.description = "<b>Failed</b> when docker was executed<br/>"
    }
}
