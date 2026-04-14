### 260414 트러블슈팅

- Jenkins 서버와 SonarQube 서버의 연결
1. Jenkins Plugin 설치

![image.png](attachment:425a32b2-4b68-4c51-b70b-710da5e39751:image.png)

1. SonarQube 토큰 발행 및 등록

SonarQube의 My Account-Security에서 토큰을 발행한다. 이후 Jenkins 메인보드에서 Credentials에 토큰을 등록한다. Secret Text로 선택하고, Scope은 global로 설정한다. Secret에 해당 토큰을 넣고 Credential을 저장한다.

1. SonarQube 서버 등록

![image.png](attachment:d05c0fab-e7fd-4b5b-b28f-974505639ac9:image.png)

Jenkins 설정에서 환경변수를 등록한다.

1. Jenkins Pipeline 생성

새로운 Pipeline을 생성한다.

![image.png](attachment:db1c9c07-eaab-41d5-a1f0-7766f0aafcaf:image.png)

![image.png](attachment:d659d0ed-632a-4826-b2e9-5b01fc55c5b4:image.png)

![image.png](attachment:a71d4677-4f6f-475c-a20a-7ba173436b51:image.png)

![image.png](attachment:c813251a-a75f-4796-9dc4-5046cdde6817:image.png)

![image.png](attachment:72369a32-58e3-4102-ac38-4449ca39fed0:image.png)

![image.png](attachment:e90d8811-e886-4bf5-bdfd-c7cae761c3fc:image.png)

![image.png](attachment:03a246aa-3b2d-4536-a0fb-4b9647bbfa5d:image.png)

1. Jenkinsfile 작성

파일명은 pipeline에서 정의한대로 작성한다. 파일의 위치는 Github repository 최상위에 있어야 한다.

```
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
                    sh 'chmod +x gradlew' // 실행 권한 부여
                    sh './gradlew sonar' 
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
```

2번의 보안 토큰은 Jenkins Pipeline에서 설정한 Token과 동일하게 작성한다. SonarQube 서버 이름도 환경변수와 동일하게 작성한다.

- checkout scm

```
stage('Checkout') {
    steps {
        checkout scm
    }
}

ERROR: ‘checkout scm’ is only available when using “Multibranch Pipeline” or “Pipeline script from SCM”
```

초기에는 Pipeline을 직접 Jenkins 대시보드에서 작성하여 동작시키는 방식을 채택했는데, 그러면 이 부분에서 위와 같은 에러가 발생했다. scm은 Source Control Management로 Jenkins가 이를 활용하여 소스로부터 직접 Jenkinsfile을 가져올 때만 이 변수에 소스에 대한 정보를 채울 수 있다. 그렇기 때문에 이를 사용하려면 파이프라인을 직접 작성하는 것이 아니라 Jenkinsfile이라는 파일에 작성해서 Github Repository의 최상위 루트에 포함시켜야 한다.

- Private Registry의 HTTPS 설정

온프레미스 환경인만큼 프로젝트의 이미지를 Docker Hub가 아닌 로컬 레지스트리를 통해 배포해보고자 했다. 하지만 여기에는 맹점이 존재하는데, docker pull 명령을 통해서 이미지를 받으려면, 해당 이미지를 제공하는 서버에 https 연결이 수행되어야 한다는 것이다. 이를 위해서 구매한 Gabia 도메인과 Cloudflare API를 이용해서 https 연결을 구현했다.

우선 Cloudflare에 Gabia에서 구매한 도메인을 등록한다. 그리고 나서 등록한 도메인에 대한 네임서버를 Gabia 네임서버에 등록한다. 이후 다시 Cloudflare에서 DNS 레코드에 A 타입으로 온프레미스 서버 IP 주소를 입력한다. 그리고 Cloudflare의 내 프로필에 대한 API 토큰을 발급받는다. Edit zone DNS를 템플릿으로 만들면서 Zone Resources를 구매한 도메인으로 선택하고 토큰을 생성한다.

```bash
docker network create sw_team_5_net

docker run -d --name sw_team_5_registry --network sw_team_5_net --restart always -e REGISTRY_STORAGE_DELETE_ENABLED=true -v $(pwd)/registry-data:/var/lib/registry registry:2

docker run -d --name sw_team_5_npm --network sw_team_5_net --restart always -p 8402:80 -p 8403:81 -p 8404:443 -v $(pwd)/npm-data:/data -v $(pwd)/npm-letsencrypt:/etc/letsencrypt jc21/nginx-proxy-manager:latest
```

온프레미스 서버에 배포를 위한 Private Registry 서버와 이를 포워딩해주기 위한 nginx proxy manager(이하 npm) 서버를 컨테이너로 띄웠다.

Proxy Host를 설정하기 위해서 npm 웹 대시보드에 접속한다. 8403번 포트를 통해 접근할 수 있다. Certificate 탭에 들어가서 Let’s Encrypt via DNS를 선택한다. 구매한 도메인 명과 이메일 주소를 선택한다. DNS Provider로 Cloudflare를 선택하고, Credentials File Content에 dns_cloudflare_api_token=Cloudflare 토큰으로 작성하여 Certificate를 생성한다.

Proxy Host 탭에서 Add Proxy Host를 수행한다. Details 탭에서는 도메인 명을 작성하고 Scheme에는 http, Forward Hostname에는 레지스트리 컨테이너명(여기서는 sw_team_5_registry), 포트는 5000에 Block Common Exploits를 설정한다. SSL 탭에서는 SSL Certificate로 방금 생성한 인증서를 선택하고, Force SSL과 HTTP/2 Support를 체크한다.

이로써 구매한 도메인으로 8084번 포트에 HTTPS 접근이 가능해졌다.

- build.gradle의 환경변수

```groovy
jib {

		from {
				image = 'eclipse-temurin:17-jre-alpine'
		}
		
		to {
				image = '172.21.33.69:8405/cicd-playground'
				tags = [buildNumber, 'latest'].unique()
				auth {
						username = System.getenv('REGISTRY_USERNAME')
						password = System.getenv('REGISTRY_PASSWORD')
				}
		}
		
		container {
				ports = ['8080']
				jvmFlags = ['-Xms512m', '-Xmx512m']
		}
		
		allowInsecureRegistries = true

}
```

초기에 작성한 build.gradle이다. 이 코드를 이용해서 빌드를 수행하면  username 부분에서 null provider와 관련한 에러가 발생한다. 즉, 환경변수가 할당되지 않은 것이다. 이는 Jenkinsfile의 실행과 관련이 있었다.

```
stage('Build & SonarQube Analysis') {
		steps {
				withSonarQubeEnv('SonarQube-Server') {
						sh 'chmod +x gradlew'
						sh './gradlew clean build sonar'
				}
		}
}
```

빌드에 앞서 SonarQube를 통해서 코드의 정적 분석을 수행해야 한다. 이 때 build.gradle을 먼저 읽어야 하는데, 해당 환경변수를 주입하는 블록이 없어서 에러가 발생하는 것이다. 따라서 수정하는 방안은 아래와 같다.

```groovy
auth {
		username = System.getenv('REGISTRY_USERNAME')
		password = System.getenv('REGISTRY_PASSWORD')
}
```

우선 이 auth 블록을 build.gradle에서 제거한다.

```
script {
    sh './gradlew jib -x test -Djib.to.auth.username=${REGISTRY_USERNAME} -Djib.to.auth.password=${REGISTRY_PASSWORD}'
}
```

그리고 Jenkinsfile에서 실행하는 스크립트에 직접 해당 환경변수 정보를 넣어주게 만들어서 문제를 해결할 수 있었다.

- JIB를 이용한 이미지의 빌드와 이미지 저장

앞서서 Private Registry에 저장된 것들을 전부 적용하고 나니 새로운 에러가 등장했다.

```
> Task :jib FAILED FAILURE: Build failed with an exception. * What went wrong: Execution failed for task ':jib'. > com.google.cloud.tools.jib.plugins.common.BuildStepsExecutionException: Tried to retrieve authentication method for 172.21.33.69:8404 but failed because: registry returned error code 400; possible causes include invalid or wrong reference. Actual error output follows: <html> <head><title>400 The plain HTTP request was sent to HTTPS port</title></head> <body> <center><h1>400 Bad Request</h1></center> <center>The plain HTTP request was sent to HTTPS port</center> <hr><center>openresty</center> </body> </html>
```

긴 에러이지만, 중요한 것은 Plain HTTP request가 HTTPS 포트로 전송되었다는 내용이다. 즉, JIB가 HTTPS가 아닌 HTTP 요청을 보냈다는 것이다. 이는 allowInsecureRegistries 옵션에서 기인하는 것으로, JIB가 SSL 인증서가 유효하지 않다고 판단하여 HTTP로 우회해서 요청을 시도한 것이다. 따라서 build.gradle에서 이 옵션을 제거해야 한다.

하지만 그럼에도 다음과 같은 에러가 발생했다.

```
PATCH https://aransword.site/v2/cicd-pipeline-playground/blobs/uploads/318b3bf4-72f1-42a6-b7d7-6b47e1dadd36?_state=생략 failed and will be retried
```

이 에러는 npm에서 발생하는 것인데, npm의 기본 설정은 들어온 요청에서 해당 요청의 포트 번호는 제거하고 서버로 요청을 전달한다. 그러면 Private Registry측에서는 요청을 처리하고 다음 조각, 즉 Blob을 보낼 주소를 전달받은 도메인인 aransword.site/v2/…, 즉 포트번호가 빠진 도메인으로 응답하게 된다. 그렇게 되면 JIB는 일반적인 HTTPS 요청을 보내므로 443 포트로 요청이 빠져서 이미지의 업로드가 실패하는 것이다.

이를 해결하기 위해 npm Proxy Host에 Custom Nginx Configuration에 다음을 추가했다.

```
proxy_set_header Host $http_host;
proxy_set_header X-Forwarded-Host $http_host;
proxy_set_header X-Forwarded-Proto $scheme;
proxy_set_header X-Forwarded-Port 8404;
```

도메인에 더해서 포트번호까지 저장소에 전달되도록 설정을 추가했다. 그럼에도 에러는 해결되지 않았다.

마지막으로 수행한 것은 Docker Registry의 설정 변경이다. Docker Registry는 다음 요청의 URL을 절대경로로 응답하는 것이 기본값인데, 이 때 포트 번호를 누락시키는 문제가 발생했다.

```bash
docker run -d --name sw_team_5_registry --network sw_team_5_net --restart always -e REGISTRY_STORAGE_DELETE_ENABLED=true -e REGISTRY_HTTP_RELATIVEURLS=true -v /opt/sw_team_5/registry-data:/var/lib/registry registry:2

```

따라서 Registry의 컨테이너를 옵션을 추가해서 생성했다. 상대경로로 응답하게 함으로써 주소는 JIB가 직접 포트를 유지하여 연결을 유지할 수 있다. 또한 저장되는 위치를 ${pwd}가 아닌 절대 경로로 설정했다.