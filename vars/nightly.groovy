import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException



def setGlobalConfig() {
    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'builder-amd', usernamevalue: 'email', passwordvalue: 'name']]) {
        sh """
            git config user.email $email
            git config user.name $name
        """
    }
}

def executeCommand(String executionType, Boolean rocmPath) {
    try {
        println("[INFO] Run unit tests")
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
        println(e.toString())
        println(e.getMessage())
        currentBuild.result = "FAILURE"
        throw e
    }
}

def executePreBuild(String stage, String script) {
    println("[INFO] Execute prebuild scripts")
    try {
        sh script
    } catch(e) {
        println(e.toString())
        println(e.getMessage())
        currentBuild.result = "FAILURE"
        throw e
    }
}

def executeStages(Map options){
    Map stages = [:]
    Map stagesMap = ["run_gpu_multi": options.preScriptMulti, "run_gpu_single": options.preScriptSingle]
    stagesMap.each() { key, value ->
        def stageName = key == "run_gpu_multi" ? "Ubuntu-GPU-multi" : "Ubuntu-GPU-single"
        stages[stageName] = {
            try{
                node("rocm"){
                    cleanWs()
                    restartDocker()
                    checkout(
                        [
                            $class: 'GitSCM',
                            userRemoteConfigs: [[url: options.repo]],
                            branches: [[name: options.branch]]
                        ]
                    )
                    stage("Execute prebuild scripts"){
                        if (value) {
                            setGlobalConfig()
                            executePreBuild(key, value)
                        }
                    }
                    stage("Execute unit tests"){
                        executeCommand(key, options.rocmPath)
                    }
                }
            } catch (e) {
                println(e.toString())
                println(e.getMessage())
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
    } catch (FlowInterruptedException e) {
        currentBuild.description = "<b style='color: #641e16'>Failure reason:</b> <span style='color: #b03a2e'>Build was aborted</span><br/>"
        currentBuild.result = "ABORTED"
    } catch(e) {
        currentBuild.result = "FAILURE"
        currentBuild.description = "<b>FAILURE</b><br/>"
    }
}
