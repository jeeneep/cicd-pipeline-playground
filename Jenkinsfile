pipeline {
    agent any
    
    triggers {
        GenericTrigger(
            // 1. GitHub Webhook JSON에서 'action' 값을 추출해 PR_ACTION 변수에 저장
            genericVariables: [
                [key: 'PR_ACTION', value: '$.action']
            ],
            // 2. 외부에서 찌를 때 사용할 보안 토큰 설정
            token: 'my-pr-close-token', 
            // 3. PR_ACTION 변수 값이 'closed'와 정확히 일치할 때만 파이프라인 실행
            regexpFilterText: '$PR_ACTION',
            regexpFilterExpression: '^closed$',
            causeString: 'Triggered by GitHub PR Closed Event'
        )
    }

    stages {
        stage('Checkout') {
            steps {
                // 1. GitHub에서 최신 소스 코드를 가져옵니다.
                checkout scm
            }
        }

        stage('Build & SonarQube Analysis') {
            steps {
                // 2. Jenkins 시스템 설정에서 등록한 SonarQube 서버 이름을 적어줍니다. (예: 'SonarQube-Server')
                withSonarQubeEnv('SonarQube-Server') { 
                    // 3. 코드를 빌드하면서 SonarQube로 분석 데이터를 보냅니다.
                    // (만약 권한 오류가 나면 'chmod +x gradlew' 명령어를 먼저 실행하도록 추가할 수 있습니다)
                    sh 'chmod +x gradlew' // 실행 권한 부여
                    sh './gradlew clean build sonar' 
                }
            }
        }
        
        stage('Quality Gate') {
            steps {
                timeout(time: 1, unit: 'MINUTES') {
                    // 4. SonarQube의 분석이 끝날 때까지 기다렸다가 통과 못하면 파이프라인 중단
                    waitForQualityGate abortPipeline: true 
                }
            }
        }

        stage('Print Hello') {
            steps {
                // 5. Quality Gate까지 모두 통과했다면 실행됩니다!
                echo 'Quality Gate Passed! Hello!'
            }
        }
    }
}