def setGlobalConfig() {
    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'builder-amd', usernameVariable: 'email', passwordVariable: 'name']]) {
        sh """
            git config user.email $email
            git config user.name $name
        """
    }
}

def executeCommand(String stage, Boolean rocmPath) {
    try {
        println("Run unit tests")
        if (rocmPath) {
            sh """
                tensorflow/tools/ci_build/ci_build.sh ROCM ./tensorflow/tools/ci_build/linux/rocm/${executionType}.sh \$ROCM_PATH
            """
        } else {
            sh """
                tensorflow/tools/ci_build/ci_build.sh ROCM ./tensorflow/tools/ci_build/linux/rocm/${executionType}.sh
            """
        }
    } catch(e) {
        throw new Exception("Failed on ${executionType} unit tests")
    }
}

def executePreBuild(String stage, String script) {
    try {
        sh script
    } catch(e) {
        throw new Exception("Failed on ${stage} pre script execution")
    }
}

def executeStages(Map options){
    Map stages = [:]
    Map stagesMap = ["run_gpu_multi": options.preScriptMulti, "run_gpu_single": options.preScriptSingle]
    stagesMap.each() { key, variable ->
        stages[key] = {
            try{
                node("rocm"){
                    restartDocker()
                    checkout(
                        [
                            $class: 'GitSCM',
                            userRemoteConfigs: [[url: options.repo]],
                            branches: [[name: options.branch]]
                        ]
                    )
                    if (variable) {
                        setGlobalConfig()
                        executePreBuild(key, variable)
                    }
                    executeCommand(key, options.rocmPath)
                }
            } catch (e) {
                currentBuild.result = "FAILURE"
                println("[ERROR] Failed on stage ${key}")
            } 
        }
    }
    parallel stages
}

def call(Map options) {
    try {
        executeStages(options)
        currentBuild.result = "SUCCESS"
        currentBuild.description = "<b>Success</b><br/>"
    } catch(e) {
        currentBuild.result = "FAILURE"
        currentBuild.description = "<b>FAILURE</b><br/>"
    }
}
