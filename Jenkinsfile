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
                    // 빌드와 분석을 먼저 수행합니다.
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
                // Quality Gate를 통과한 경우에만 이미지를 빌드하고 푸시합니다.
                withCredentials([usernamePassword(credentialsId: 'registry-auth',
                                                  passwordVariable: 'REGISTRY_PASSWORD',
                                                  usernameVariable: 'REGISTRY_USERNAME')]) {
                    script {
                        // 이미 위 단계(build sonar)에서 테스트를 완료했으므로,
                        // jib 단계에서는 테스트를 생략하고 빌드 시간만 단축할 수 있습니다.
                        sh './gradlew jib -x test -Djib.to.auth.username=${REGISTRY_USERNAME} -Djib.to.auth.password=${REGISTRY_PASSWORD}'
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