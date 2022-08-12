
def executeCommand(String executionType) {
    sh """
        tensorflow/tools/ci_build/ci_build.sh ROCM ./tensorflow/tools/ci_build/linux/rocm/${executionType}.sh
    """
}

def checkoutProject(String repo, String branch){
    checkout(
        [
            $class: 'GitSCM',
            userRemoteConfigs: [[url: repo]],
            branches: [[name: branch]]
        ]
    )
}

if (env.GIT_BRANCH && env.GIT_URL) {
    String branch = env.GIT_BRANCH
    String repo = env.GIT_URL
} else {
    String branch="develop-upstream"
    String repo="https://github.com/ROCmSoftwarePlatform/tensorflow-upstream/"
}

pipeline {
    options {
        timeout(time: 10, unit: 'HOURS')
        timestamps()
    }
    parallel {
        stage("cpu") {
            node("rocm"){
                checkoutProject(repo, branch)
                executeCommand("run_cpu") 
            }
        },
        stage("gpu_multi") {
            node("rocm"){
                checkoutProject(repo, branch)
                executeCommand("run_gpu_multi") 
            }
        },
        stage("gpu_single") {
            node("rocm"){
                checkoutProject(repo, branch)
                executeCommand("run_gpu_single") 
            }
        },
        stage("ci_sanity") {
            node("rocm"){
                checkoutProject(repo, branch)
                executeCommand("rocm_ci_sanity") 
            }
        }
    }
    post {
        success {
            print "SUCCESS"
        }
        failure {
            print "FAILURE"
        }
        unstable {
            print "UNSTABLE"
        }
        cleanup {
            cleanWs()
            sh "docker stop ${CONTAINER_NAME} || true"
            sh "docker rm -v ${CONTAINER_NAME} || true"
        }
    }
}