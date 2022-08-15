def executeCommand(String executionType) {
    try {
        /*
        println("Build executed")
        sh """
            tensorflow/tools/ci_build/ci_build.sh ROCM ./tensorflow/tools/ci_build/linux/rocm/${executionType}.sh
        """
        */
    } catch(e) {
        throw new Exception("Failed on ${executionType} stage")
    }
}

def executeStages(){
    if (env.GIT_BRANCH && env.GIT_URL) {
        String branch = env.GIT_BRANCH
        String repo = env.GIT_URL
    } else {
        String branch="develop-upstream"
        String repo="https://github.com/ROCmSoftwarePlatform/tensorflow-upstream/"
    }

    Map stages = [:]
    List listOfStages = ["run_cpu", "run_gpu_multi", "run_gpu_single", "rocm_ci_sanity"]
    listOfStages.each() {
        stages[it] = {
            try{
                def label = "rocm"
                if (branch.contains("r2.9") && it == "run_gpu_multi"){
                    label += "&&multi_gpu"
                } 
                node(label){
                    restartDocker()
                    checkout(
                        [
                            $class: 'GitSCM',
                            userRemoteConfigs: [[url: repo]],
                            branches: [[name: branch]]
                        ]
                    )
                    executeCommand(it)
                }
            } catch (e) {
                currentBuild.result = "FAILURE"
                println("[ERROR] Failed on stage ${it}")
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
        currentBuild.description = "<b>FAILURE</b><br/>"
    }
}
