def gitRepo = "https://github.com/ROCmSoftwarePlatform/tensorflow-upstream/"
def creds = "Token"
def setUpDocker(){
    println("Set up docker")
    // Example with simply shell script
    sh """
        systemctl status docker | grep 'Active:'
        sudo /usr/bin/pkill -f docker
        sudo /bin/systemctl restart docker
        docker system prune -a -f
        systemctl status docker | grep 'Active:'
    """
}

def executeCommand(String parameter) {
    println("Build executed")
    sh """
        build/tensorflow/tools/ci_build/ci_build.sh ROCM ./build/tensorflow/tools/ci_build/linux/rocm/run_${parameter}.sh
    """
}

def call() {
    node("rocm"){
        stage("Set up Docker"){
            setUpDocker()
        }
        stage("Clone project"){
            checkout([$class: 'GitSCM',
                    userRemoteConfigs: [[url: gitRepo, credentialsId: creds]],
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

call()


def masterBuildOnly(){
    sh """
        sed -i 's|ROCM_EXTRA_PARAMS="--device=/dev/kfd --device=/dev/dri --group-add video |ROCM_EXTRA_PARAMS="|g' tensorflow/tools/ci_build/ci_build.sh
        BUILD_SCRIPT=build_rocm_python3
        cat <<EOF > \$BUILD_SCRIPT
        #!/bin/bash
        set -eux
        ROCM_PATH=/opt/rocm-5.1.0
        yes "" | ROCM_PATH=\$ROCM_PATH TF_NEED_ROCM=1 PYTHON_BIN_PATH=/usr/bin/python3 ./configure
        cat .tf_configure.bazelrc
        bazel build --config=rocm //tensorflow/tools/pip_package:build_pip_package --verbose_failures
        bazel-bin/tensorflow/tools/pip_package/build_pip_package /tmp/tensorflow_pkg
        EOF
        chmod a+x ./\$BUILD_SCRIPT
        tensorflow/tools/ci_build/ci_build.sh ROCM ./\$BUILD_SCRIPT
    """
}