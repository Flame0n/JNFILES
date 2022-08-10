@Field final String SCRIPT = '''#!/bin/bash
sed -i 's|ROCM_EXTRA_PARAMS="--device=/dev/kfd --device=/dev/dri --group-add video |ROCM_EXTRA_PARAMS="|g' tensorflow/tools/ci_build/ci_build.sh
touch build_rocm_python3
echo "
set -eux
ROCM_PATH=/opt/rocm-5.2.0
yes \\"\\" | ROCM_PATH=/opt/rocm-5.2.0 TF_NEED_ROCM=1 PYTHON_BIN_PATH=/usr/bin/python3 ./configure
cat .tf_configure.bazelrc
bazel build --config=rocm //tensorflow/tools/pip_package:build_pip_package --verbose_failures
bazel-bin/tensorflow/tools/pip_package/build_pip_package /tmp/tensorflow_pkg
    " >> build_rocm_python3
chmod a+x ./build_rocm_python3
tensorflow/tools/ci_build/ci_build.sh ROCM ./build_rocm_python3
'''

def buildjob(){
    sh SCRIPT
}
            
def call() {
    timeout(time: 10, unit: 'HOURS') {
        node("tensorflow-ci"){
            try {
                githubNotify status: "PENDING", context: "AMD ROCm -- Community CI Build ", description: "build started", credentialsId: "46665a52-3ecc-40e8-8435-cc65202ecae5", account: "tensorflow", repo: "tensorflow"
                buildjob()
                githubNotify status: "SUCCESS", context: "AMD ROCm -- Community CI Build ", description: "rocm CI build successful", credentialsId: "46665a52-3ecc-40e8-8435-cc65202ecae5", account: "tensorflow", repo: "tensorflow"
            } catch (e) {
                currentBuild.result = "FAILED"
                githubNotify status: "FAILURE", context: "AMD ROCm -- Community CI Build ", description: "rocm CI build failed", credentialsId: "46665a52-3ecc-40e8-8435-cc65202ecae5", account: "tensorflow", repo: "tensorflow"
                failureMessage = "BUILD FAILED"
            } finally {
                cleanWs()
            }
        }
    }
}