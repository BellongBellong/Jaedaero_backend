# 로컬 k6 모니터링

이 디렉터리는 성능 테스트에서만 Prometheus, Grafana, Tomcat JMX exporter,
MySQL exporter를 추가한다. 운영 배포에는 이 구성을 사용하지 않는다.

## 준비

1. JMX exporter agent를 내려받는다.

```sh
./monitoring/download-jmx-exporter.sh
```

2. 로컬 성능 환경 변수 파일을 만든다.

```sh
cp monitoring/.env.performance.example .env.performance
```

`.env.performance`의 모든 `CHANGE_ME` 값을 바꾼다. `JWT_SECRET`에는 다음 명령으로
만든 값을 넣는다.

`CODEF_CLIENT_ID`, `CODEF_CLIENT_SECRET`은 애플리케이션 빈 생성에 필요하다. 이번 성능
테스트가 CODEF API를 직접 호출하지 않는다면 로컬 전용 임의 문자열을 넣어도 된다.
`APP_CRYPTO_MASTER_KEY_BASE64`는 `openssl rand -base64 32`로 별도 생성한다.

```sh
openssl rand -base64 32
```

MySQL exporter 계정은 MySQL 컨테이너 최초 초기화 시 자동 생성된다.

## 실행

```sh
docker compose \
  --env-file .env.performance \
  -f docker-compose.performance.local.yml \
  up -d --build
```

- Grafana: http://localhost:3000
- Prometheus: http://localhost:9090
- 성능 MySQL: `127.0.0.1:13306`
- 백엔드 JMX metrics: Docker 네트워크 내부 `backend:9404`
- MySQL metrics: Docker 네트워크 내부 `mysqld-exporter:9104`

Prometheus 데이터 소스는 Grafana에 자동 등록된다.

## k6 결과 전송

```sh
K6_PROMETHEUS_RW_SERVER_URL=http://localhost:9090/api/v1/write \
K6_PROMETHEUS_RW_TREND_STATS='p(95),p(99),avg,max' \
k6 run -o experimental-prometheus-rw \
  --tag testid=challenge-10k-read \
  k6/challenge-read.js
```

`testid`는 실행마다 바꿔서 Grafana에서 결과를 구분한다.

## 종료와 초기화

```sh
docker compose --env-file .env.performance -f docker-compose.performance.local.yml down
```

성능 DB까지 완전히 초기화하려면 아래 명령을 사용한다. 모든 성능 테스트 데이터가 삭제된다.

```sh
docker compose --env-file .env.performance -f docker-compose.performance.local.yml down -v
```
