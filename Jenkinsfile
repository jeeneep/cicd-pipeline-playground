pipeline {
    agent any
    
    triggers {
        GenericTrigger(
            genericVariables: [
                [key: 'PR_ACTION', value: '$.action']
            ],
            token: 'my-pr-close-token',
            regexpFilterText: '$PR_ACTION',
            regexpFilterExpression: '^closed$',
            causeString: 'Triggered by GitHub PR Closed Event'
        )
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube-Server') {
                    sh 'chmod +x gradlew'
                    sh './gradlew clean build sonar'
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Docker Build & Push (Jib)') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'registry-auth',
                                                  passwordVariable: 'REGISTRY_PASSWORD',
                                                  usernameVariable: 'REGISTRY_USERNAME')]) {
                    script {
                        sh "./gradlew jib -Djib.to.auth.username=${REGISTRY_USERNAME} -Djib.to.auth.password=${REGISTRY_PASSWORD} -x test"
                    }
                }
            }
        }

        stage('Print Hello') {
            steps {
                echo 'Build, Sonar Analysis, and Docker Push are all SUCCESSFUL!'
            }
        }
    }
}