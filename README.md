# 제대로(JaedaeRo) Backend

군인의 확정 소득과 금융 데이터를 바탕으로 전역 예상 자산을 계산하고, 자산 관리와 AI 금융 코칭을 제공하는 제대로의 백엔드입니다.

Spring Boot를 사용하지 않는 Spring MVC 기반 WAR 애플리케이션이며 Tomcat 9에 배포합니다.

## 주요 기능

- Google·Kakao 소셜 로그인, JWT 인증, 온보딩과 사용자 프로필
- CODEF 계좌 연동, 계좌·거래 내역 조회와 거래 카테고리 분류
- 현금흐름 예측, What-if 시뮬레이션, AI 분석과 추천 전략 적용
- 목표 관리, 챌린지·미션·랭킹, 휴가 모드와 군인 혜택
- 적립식 투자 가이드, ETF·증권 포트폴리오, 일일 시장 리포트
- Redis Streams Outbox와 Firebase Cloud Messaging 기반 알림
- 일일 데이터·시장 리포트·알림 배치 작업

REST API 계약은 [api-endpoint-mapping.md](api-endpoint-mapping.md)를 기준으로 합니다.

## 기술 스택

| 구분 | 기술 |
| --- | --- |
| Language | Java 17 |
| Build | Gradle Wrapper 9.3, WAR |
| Framework | Spring Framework 5.3.39, Spring MVC, Spring Security 5.8.15 |
| Data | MyBatis 3.5.19, MyBatis-Spring 2.1.2, MySQL 8, HikariCP |
| Auth | OAuth 2.0, JWT |
| Messaging | Redis Streams, Firebase Cloud Messaging |
| API 문서 | Springfox Swagger 2 |
| Runtime | Tomcat 9, Docker Compose |
| Test | JUnit 5, Spring Test, H2 |

## 아키텍처

요청은 다음 계층을 따릅니다.

```text
Controller -> Service -> ServiceImpl -> Mapper -> MyBatis XML -> MySQL
```

- Java Config가 루트·MVC·Security 컨텍스트를 구성합니다.
- Mapper 인터페이스는 `src/main/java/**/mapper`, SQL은 `src/main/resources/mapper`에 둡니다.
- DTO는 API 요청·응답, VO는 영속성 모델에 사용합니다.
- 공통 예외는 `global/common/exception`, 인증·암호화 기능은 `global/security`에서 관리합니다.

## 프로젝트 구조

```text
src/main
├── java/com/jaedaero
│   ├── global
│   │   ├── batch                 # 정기 데이터·알림 작업
│   │   ├── common                # 공통 응답, 예외, 유틸리티
│   │   ├── config                # MVC, DB, Security, Swagger 설정
│   │   └── security              # JWT와 민감정보 암호화
│   └── domain
│       ├── auth                  # 인증·온보딩
│       ├── codef                 # 금융기관·계좌·거래 연동
│       ├── dashboard             # 홈 대시보드
│       ├── cashflow              # 현금흐름 예측
│       ├── simulation            # What-if 시뮬레이션
│       ├── aianalysis            # AI 분석
│       ├── analysishistory       # 분석 통합 이력
│       ├── strategyapplication   # 추천 전략 적용
│       ├── goal                  # 목표 관리
│       ├── challenge             # 그룹·랭킹·미션
│       ├── investment            # ETF·상품 정보
│       ├── investmentguidance    # 투자 가이드
│       ├── recurringinvestment   # 적립식 투자 계획
│       ├── marketreport          # 일일 시장 리포트
│       ├── notification          # 알림·FCM·Redis Streams
│       ├── leavebenefit          # 군인 휴가 혜택
│       ├── leavemode             # 휴가 모드
│       ├── mypage                # 사용자 정보
│       └── report                # 전역 리포트
└── resources
    ├── mapper                    # 도메인별 MyBatis Mapper XML
    ├── application.properties   # 공통 기본값
    ├── application-local.properties
    ├── mybatis-config.xml
    └── log4j2.xml
```

## 로컬 실행

### 사전 준비

- JDK 17
- MySQL 8
- Tomcat 9
- DB 초기화 작업을 사용할 경우 MySQL CLI

### 1. 애플리케이션 설정

```powershell
Copy-Item src/main/resources/application-local.properties.example `
  src/main/resources/application-local.properties
```

macOS/Linux에서는 다음 명령을 사용합니다.

```bash
cp src/main/resources/application-local.properties.example \
  src/main/resources/application-local.properties
```

복사한 파일에 로컬 DB 연결 정보와 사용하는 외부 API 키를 입력합니다. 애플리케이션 시작에 필요한 핵심 값은 다음과 같습니다.

- `db.driver`, `db.url`, `db.username`, `db.password`
- `app.crypto.master-key-base64`: Base64로 인코딩한 32바이트 AES-256 키
- `JWT_SECRET`: Base64로 인코딩한 32바이트 이상의 JWT 키

소셜 로그인과 CODEF 자격 증명은 저장소 루트의 `.env`에서 읽습니다. `.env`에는 `JWT_SECRET`, `GOOGLE_*`, `KAKAO_*`, `CODEF_*` 등 사용하는 기능의 환경변수를 설정합니다. 외부 API를 사용하지 않는 기능의 키는 예시 파일의 빈 값이나 로컬용 값을 유지할 수 있습니다.

`application-local.properties`와 `.env`의 실제 비밀값은 커밋하지 않습니다.

### 2. 데이터베이스 초기화

아래 명령은 지정한 로컬 DB의 기존 테이블과 데이터를 삭제하고 스키마·운영 기준 데이터를 다시 적재합니다.

```powershell
.\gradlew.bat dbReset -PconfirmLocalDb=jaedaero
```

개발·QA 목데이터까지 필요하면 다음 작업을 사용합니다.

```powershell
.\gradlew.bat dbResetWithMock -PconfirmLocalDb=jaedaero
```

세부 SQL 적용 기준과 개별 작업은 [sql/README.md](sql/README.md)를 확인합니다.

### 3. 테스트와 WAR 빌드

```powershell
.\gradlew.bat test
.\gradlew.bat war
```

macOS/Linux에서는 `./gradlew`를 사용합니다. 생성 파일은 `build/libs/jaedaero-1.0-SNAPSHOT.war`입니다.

### 4. Tomcat 실행

API를 루트 경로(`/api/v1/...`)로 실행하려면 WAR 파일을 Tomcat의 `webapps/ROOT.war`로 배포합니다.

```powershell
Copy-Item build/libs/jaedaero-1.0-SNAPSHOT.war "$env:CATALINA_HOME/webapps/ROOT.war"
& "$env:CATALINA_HOME/bin/catalina.bat" run
```

실행 후 확인할 수 있는 주소는 다음과 같습니다.

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- Swagger JSON: <http://localhost:8080/v2/api-docs>

## Docker·운영 배포

`Dockerfile`은 Gradle로 WAR를 빌드한 뒤 Tomcat 9의 `ROOT.war`로 실행합니다. `docker-compose.prod.yml`은 백엔드와 MySQL 8.4를 구성하며, 백엔드는 로컬 호스트의 `127.0.0.1:8080`에만 바인딩됩니다.

운영 배포 절차는 다음 문서를 따릅니다.

- [EC2 Docker Compose 배포](docs/ec2-docker-deployment.md)
- [GitHub Actions + AWS SSM 자동 배포](docs/github-actions-ssm-deployment.md)

## 테스트·성능 확인

```powershell
.\gradlew.bat test
```

- 단위·통합 테스트 결과: `build/reports/tests/test/index.html`
- 부하 테스트: [k6/README.md](k6/README.md)
- Prometheus·Grafana 모니터링: [monitoring/README.md](monitoring/README.md)
