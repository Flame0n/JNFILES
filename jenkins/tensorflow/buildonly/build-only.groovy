def executeBuild(){
    sh """#!/bin/bash -x

        # Build-only mod:  remove docker's use of kfd and dri devices
        sed -i 's|ROCM_EXTRA_PARAMS="--device=/dev/kfd --device=/dev/dri --group-add video |ROCM_EXTRA_PARAMS="|g' tensorflow/tools/ci_build/ci_build.sh

        BUILD_SCRIPT=build_rocm_python3

        cat <<_EOF_ > ./\$BUILD_SCRIPT
        ""#!/bin/bash

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

def checkoutProject(String repo, String branch){
    checkout(
        [
            $class: 'GitSCM',
            userRemoteConfigs: [[url: repo]],
            branches: [[name: branch]]
        ]
    )
}

pipeline {
    agent {
        node { label 'build-only' }
    }
    options {
        timeout(time: 3, unit: 'HOURS')
        timestamps()
    }
    stages {
        stage('Clone repository') {   
            steps {
                checkoutProject("https://github.com/tensorflow/tensorflow.git", "master")
            }
        }
        stage('Execute build') {   
            steps {
                executeBuild()
            }
        }
    }
    post {
        cleanup {
            cleanWs()
        }
    }
}