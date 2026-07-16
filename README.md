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
