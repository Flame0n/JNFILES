def executeBuild(){
    sh """

    bazel version 

        
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