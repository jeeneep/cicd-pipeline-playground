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
                        sh './gradlew jib -x test -Djib.to.auth.username=${REGISTRY_USERNAME} -Djib.to.auth.password=${REGISTRY_PASSWORD} --stacktrace --info'
                    }
                }
            }
        }

        // 3. 배포 서버(.26)로 스크립트 전송 및 실행 (주호 님이 말씀하신 핵심!)
        stage('Remote Blue-Green Deploy') {
            steps {
                // 'front-com-key'는 젠킨스 Credential에 등록한 .26 서버 접속 ID여야 합니다.
                sshagent(credentials: ['front-com-key']) {
                    script {
                        def remoteServer = "sw_team_5@172.21.33.26"
                        def imageTag = "${env.BUILD_NUMBER}"

                        // [과정 B] 배포 서버(.26)에 원격 접속해서 스크립트 실행 명령
                        // 이때 주호님이 작성하신 도커 실행/헬스체크/Nginx 리로드가 .26 서버 터미널에서 실행됩니다.
                        sh """
                            ssh -o StrictHostKeyChecking=no ${remoteServer} '
                                chmod +x /home/sw_team_5/deploy.sh &&
                                /home/sw_team_5/deploy.sh ${imageTag}
                            '
                        """
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