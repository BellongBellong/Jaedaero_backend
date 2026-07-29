# 제대로 Backend

군인의 확정소득을 기반으로 전역 예상 자산을 계산하고 AI 금융 코칭을 제공하는 `제대로(JaedaeRo)`의 백엔드 저장소입니다.

## 기술 스택

- Java 17, Gradle, Spring Framework 5.3
- Tomcat 9, MyBatis, MySQL
- Spring Security, OAuth 2.0, JWT

## 실행 전 준비

1. `src/main/resources/application-local.properties.example`을 복사해 `application-local.properties`를 만듭니다.
2. 로컬 MySQL 및 외부 API 환경변수를 설정합니다.
3. Tomcat 9에 WAR 파일을 배포합니다.

```bash
./gradlew test
./gradlew war
```

`application-local.properties`와 API 키는 Git에 커밋하지 않습니다.

## 패키지 구조

```text
com.jaedaero
├── global
│   ├── config       # DB, Swagger 등 전역 설정
│   └── security     # 암호화·해시 등 공통 보안 기능
└── domain
    ├── codef        # 금융기관 연결, 계좌, 증권, 토큰, 데모 화면
    │   ├── account
    │   ├── client
    │   ├── connection
    │   ├── demo
    │   ├── exception
    │   ├── institution
    │   ├── persistence
    │   └── token
    └── investment
        ├── etf      # KRX ETF 시세·수익률 조회
        └── exception
```
