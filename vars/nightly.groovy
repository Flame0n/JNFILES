import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException

def DEFAULT_CONFIG = [rocmPath: false]

def NIGHTLY_ROCMFORK_DEVELOP_UPSTREAM = [
    rocmPath: true,
    preScriptMulti : """
        # Enable multi-gpu tests
        git cherry-pick 12a7be11e8ef5914f72d20b2a29ac24f240cf788

        #Enable collective_nccl test
        #git cherry-pick 0cd998e300a86cae5b8724a09aac860d0c7c9ad4
    """
]

def NIGHTLY_UPSTREAM_R29 = [
    rocmPath: false,
    preScriptMulti : """
        git remote add rocm http://github.com/ROCmSoftwarePlatform/tensorflow-upstream
        git fetch rocm

        #Disable gpu_device_unified_memory
        git cherry-pick f556cefb6b48ff97f03582cf99e34250149a5167

        #Update run_multi_gpu 
        git cherry-pick 4dd2a53b392c5252e3b012f7b8fcae23805556c4
    """,
    preScriptSingle : """
        git remote add rocm http://github.com/ROCmSoftwarePlatform/tensorflow-upstream
        git fetch rocm
        git remote add jayfurmanek http://github.com/jayfurmanek/tensorflow
        git fetch jayfurmanek

        #Disable config test
        git cherry-pick 89843eae270ac11c67dd82d7edc3a0d5570f4d35

        #Disable device test
        git cherry-pick 9676f1fa93220d8233d7de58e590e8dfe02323b9

        #Matmul
        #git cherry-pick 9a44bc7bcaf75802ca558dcd60cf95ccffd9fa88
        #git cherry-pick a60fef954a54c7468575ae5f743f33a7a59243ac

        #PR forthcoming disable argminmax_gpu
        git cherry-pick 6989718c1dd07ba66f966eeafb10e97c89a76255

        #Disable gpu_device_unified_memory
        git cherry-pick f556cefb6b48ff97f03582cf99e34250149a5167
    """
]

def NIGHTLY_UPSTREAM_MASTER = [
    rocmPath: true,
    preScriptMulti : """
        git remote add rocm http://github.com/ROCmSoftwarePlatform/tensorflow-upstream
        git fetch rocm
        git remote add jayfurmanek http://github.com/jayfurmanek/tensorflow
        git fetch jayfurmanek

        #Cherry-pick ROCm5.2 build fix commits
        git cherry-pick 7cf39d0afa25b086bf57c5a681b63befef5041cf
        git cherry-pick 982dff8997bcc945a89bd36912065a7ada5da2e0
        git cherry-pick 93e1b98e215e0399ac0e35bc77ad66544043e7f9
        git cherry-pick 6d760cc000fe5f795c3840c800f61ddc62353d63
        git cherry-pick e33876297b6fbe79dac8ff3dc6ed182803af7158
        git cherry-pick ef8d7e8b949ee52a9c84058774cd500418eaf9e9

        #Disable gpu_device_unified_memory
        git cherry-pick f556cefb6b48ff97f03582cf99e34250149a5167

        #Update run_multi_gpu 
        git cherry-pick 4dd2a53b392c5252e3b012f7b8fcae23805556c4
    """,
    preScriptSingle : """
        git remote add rocm http://github.com/ROCmSoftwarePlatform/tensorflow-upstream
        git fetch rocm
        git remote add jayfurmanek http://github.com/jayfurmanek/tensorflow
        git fetch jayfurmanek

        # Disable qr_op_test as it is running out of memory
        git cherry-pick fe493bff5617cf64ac932ae1df701e9f8ecfd8c5

        #PR forthcoming disable argminmax_gpu
        git cherry-pick 6989718c1dd07ba66f966eeafb10e97c89a76255

        #Fix for fusion logical test
        #git cherry-pick bafc744d45ca9d98528ab0bb0b6b92679ca99eca

        #Disable device test
        git cherry-pick 9676f1fa93220d8233d7de58e590e8dfe02323b9

        #Fix Backward Pooling
        #git cherry-pick 84df317a55651dd6561fd9d9753678f2b8f84565

        #Disable config test
        git cherry-pick 89843eae270ac11c67dd82d7edc3a0d5570f4d35

        #Disable auto-mixed precision(This is passing now, but leaving it just in case we see again)
        git cherry-pick 03ab8d6dc5fd7025855565717896c35b6707eb5a
        git cherry-pick 8ababff37bd04af8c903c4d542a2547a920c17d4 #Add July 26th, 2022
        

        #fusion matmul_op_test fix
        #git cherry-pick da8368a74c809e139fa467da94d091b4a3ec99d2 //COmpletely disable matmul_op_test
        git cherry-pick 95b047837ea43fd2fca9a4a4aba080ff3d215008 #Fix to not run just the FusedMatMul op test on AMD GPU/ROCm

        #Disable CUDNN deterministic test
        git cherry-pick bf813892b988e584a526123dba5648aca6b7d3b7
        git cherry-pick 5e903448d8d40d7265fa6b46526ded3b7f990062

        #Grappler remapper
        git cherry-pick 996ce5619e420b975b736f37b21d98d28c9f095c

        #ROCM 5.2 build support
        git cherry-pick 7cf39d0afa25b086bf57c5a681b63befef5041cf
        git cherry-pick 982dff8997bcc945a89bd36912065a7ada5da2e0
        git cherry-pick 93e1b98e215e0399ac0e35bc77ad66544043e7f9 
        git cherry-pick 6d760cc000fe5f795c3840c800f61ddc62353d63
        git cherry-pick e33876297b6fbe79dac8ff3dc6ed182803af7158
        git cherry-pick ef8d7e8b949ee52a9c84058774cd500418eaf9e9

        #Matmul
        git cherry-pick 9a44bc7bcaf75802ca558dcd60cf95ccffd9fa88
        git cherry-pick a60fef954a54c7468575ae5f743f33a7a59243ac

        #GPU atomics
        git cherry-pick 79d77bd0de73ea81157e56d11e080144ca23d705

        #python/eager/def_function_xla_jit_test_gpu
        git cherry-pick d6d3e3ef3525ecd502f428fbca931c5d47221777

        #runtime_shape_check_test_gpu
        git cherry-pick b033a97ca7a9a85db0efb2f80f266aa9d9a27e17

        #Image ops jit compile
        git cherry-pick 9bd4458d387a2ed6f5ed9b4c0764333c54c21d1a

        #Do not enable MLIR-based rewrite of lmhlo.fusion ops by default on ROCM
        git cherry-pick a4c9b9f338b9099f088592423404b2b1aae555d1
    """
]

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

def executePreBuild(String script) {
    println("[INFO] Execute prebuild scripts")
    try {
        sh script
    } catch(e) {
        println(e.toString())
        println(e.getMessage())
        throw e
    }
}

def executeStages(){
    switch(params.branch) {
        case "develop-upstream":
            def config = NIGHTLY_ROCMFORK_DEVELOP_UPSTREAM
            break
        case "master":
            def config = NIGHTLY_UPSTREAM_MASTER
            break
        case "r2.9":
            def config = NIGHTLY_UPSTREAM_R29
            break
        default:
            def config = DEFAULT_CONFIG
            break 
    }

    Map stages = [:]
    Map stagesMap = ["run_gpu_multi": config.preScriptMulti, "run_gpu_single": config.preScriptSingle]
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
                            userRemoteConfigs: [[url: params.repo]],
                            branches: [[name: params.branch]]
                        ]
                    )
                    stage("Execute prebuild scripts"){
                        if (value) {
                            setGlobalConfig()
                            executePreBuild(key, value)
                        }
                    }
                    stage("Execute unit tests"){
                        executeCommand(key, config.rocmPath ?: false)
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

def call() {
    try {
        executeStages()
    } catch (FlowInterruptedException e) {
        currentBuild.description = "<b style='color: #641e16'>Failure reason:</b> <span style='color: #b03a2e'>Build was aborted</span><br/>"
        currentBuild.result = "ABORTED"
    } catch(e) {
        currentBuild.result = "FAILURE"
        currentBuild.description = "<b>FAILURE</b><br/>"
    }
}
