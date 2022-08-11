def call() {
    def TF_CLONE_DIR = 'tensorflow'
    def TF_ARTIFACTS_DIR = 'artifactory'
    def DOCKER_IMAGES = ['rocm/tensorflow-autobuilds:ubuntu20.04-rocm5.2.0-multipython']
    def DOCKER_RUN_OPTIONS = "\
    --network=host \
    --ipc=host \
    --shm-size 16G \
    --group-add video \
    --cap-add=SYS_PTRACE \
    --security-opt seccomp=unconfined \
    --device=/dev/kfd\
    --device=/dev/dri \
    "
    def tensorflowRepo = ''
    def tensorflowBranch= ''

    if (env.JOB_NAME.contains("master")){
        tensorflowRepo = 'https://github.com/tensorflow/tensorflow'
        tensorflowBranch= 'master'
    } else {
        tensorflowRepo = 'https://github.com/ROCmSoftwarePlatform/tensorflow-upstream'
        if (env.JOB_NAME.contains("r2.8-rocm-enhanced")) {
            tensorflowBranch= 'r2.8-rocm-enhanced'
        } else if (env.JOB_NAME.contains('r2.9-rocm-enhanced')) {
            tensorflowBranch= 'r2.9-rocm-enhanced'
        } else if (env.JOB_NAME.contains('r2.10-rocm-enhanced')) {
            tensorflowBranch= 'r2.10-rocm-enhanced'
        } else if (env.JOB_NAME.contains('develop-upstream')) {
            tensorflowBranch= 'develop-upstream'
        } else {
            throw new Exception("Failed to parse project repo and branch")
        }
    }

    if (tensorflowBranch== "master" || tensorflowBranch== "develop-upstream") {
        DOCKER_RUN_OPTIONS += "-e IS_NIGHTLY=1"
    }

    if (tensorflowBranch == 'master') {
        DOCKER_IMAGES = ['rocm/tensorflow-autobuilds:ubuntu18.04-rocm5.0.0-multipython']
    } else if (tensorflowBranch == 'r2.8-rocm-enhanced' || tensorflowBranch == 'develop-upstream') {
        DOCKER_IMAGES << 'rocm/tensorflow-autobuilds:ubuntu18.04-rocm5.1.0-multipython'
    } else if (tensorflowBranch == 'r2.9-rocm-enhanced' || tensorflowBranch == 'r2.10-rocm-enhanced') {
        DOCKER_IMAGES << 'rocm/tensorflow-autobuilds:ubuntu18.04-rocm5.1.3-multipython'
    }

    def stages = [:]

    DOCKER_IMAGES.each() {

        stages[it] = {
            node("rocm") {
                try {
                    timeout(time: 12, unit: 'HOURS') {
                        stage("Pull docker image") {
                            println('[INFO] Pull the ROCm multipython docker image')
                            sh "docker pull ${it}"
                        }
                        
                        stage("Clone ROCm TF") {
                            println("[INFO] Clone ROCm TF Github tensorflowRepo")

                            checkout(
                                [
                                    $class: 'GitSCM',
                                    userRemoteConfigs: [[url: tensorflowRepo]],
                                    branches: [[name: tensorflowBranch]],
                                    extensions: [[$class: "RelativeTargetDirectory", relativeTargetDir: TF_CLONE_DIR]]
                                ]
                            )
                        }

                        sh "mkdir -p ${TF_ARTIFACTS_DIR}"

                        dir("${TF_CLONE_DIR}") {
                            stage("Python 3.7 whl") {
                                println("[INFO] Build the Python 3.7 whl")
                                sh "docker run \
                                        ${DOCKER_RUN_OPTIONS} \
                                        -v ${WORKSPACE}/${TF_CLONE_DIR}:/tensorflow \
                                        -w /tensorflow \
                                        ${it} \
                                        ./tensorflow/tools/ci_build/linux/rocm/rocm_py37_pip.sh"

                                sh "cp pip_test/whl/* ${TF_ARTIFACTS_DIR}/"
                            }

                            stage("Python 3.8 whl") {
                                println("[INFO] Build the Python 3.8 whl")
                                sh "docker run \
                                        ${DOCKER_RUN_OPTIONS} \
                                        -v ${WORKSPACE}/${TF_CLONE_DIR}:/tensorflow \
                                        -w /tensorflow \
                                        ${it} \
                                        ./tensorflow/tools/ci_build/linux/rocm/rocm_py38_pip.sh"

                                sh "cp pip_test/whl/* ${TF_ARTIFACTS_DIR}/"
                            }

                            stage("Python 3.9 whl") {
                                println("[INFO] Build the Python 3.9 whl")
                                sh "docker run \
                                        ${DOCKER_RUN_OPTIONS} \
                                        -v ${WORKSPACE}/${TF_CLONE_DIR}:/tensorflow \
                                        -w /tensorflow \
                                        ${it} \
                                        ./tensorflow/tools/ci_build/linux/rocm/rocm_py39_pip.sh"

                                sh "cp pip_test/whl/* ${TF_ARTIFACTS_DIR}/"
                            }                        
                        }

                        stage("Rename whls") {
                            println("[INFO] Rename whls")
                            sh "#!/usr/bin/env bash \n" +
                                "set -eux && cd ${TF_ARTIFACTS_DIR} && " +
                                        'for filename in *.whl; do \
                                        new_filename=${filename//linux/manylinux1}; \
                                        mv $filename $new_filename; \
                                        done'                        
                        }
                    }
                } catch(e) {
                    println("[ERROR] FAILED on ${it} stage")
                    currentBuild.result = "FAILURE"
                } finally {
                    archiveArtifacts artifacts: "${TF_ARTIFACTS_DIR}/*", fingerprint: true
                    cleanWs()
                }
            }
        }
    }

    parallel stages
}
