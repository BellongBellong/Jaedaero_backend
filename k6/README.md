# CODEF 실연동 제외 k6 실행

이 테스트 세트는 CODEF 연동·계좌 연결·거래내역 API를 호출하지 않는다. 거래내역 API는 누락된
조회 기간이 있으면 CODEF 동기화를 수행할 수 있어 성능 테스트 대상에서 의도적으로 제외했다.
대상은 로컬 DB만 사용하는 챌린지 랭킹 조회, 대시보드 조회, 미션 완료 처리다.

현재 성능 DB의 `k6-load-000001` 사용자 ID는 `1169561`이다. 재생성 뒤에는 아래 쿼리로
시작 ID를 다시 확인하고 `K6_USER_ID_START`에 넣는다.

```sql
SELECT user_id
FROM users
WHERE social_id = 'k6-load-000001';
```

## 챌린지 랭킹 조회

```sh
K6_USER_ID_START=1169561 \
K6_MAX_VUS=100 \
K6_PROMETHEUS_RW_SERVER_URL=http://localhost:9090/api/v1/write \
K6_PROMETHEUS_RW_TREND_STATS='p(95),p(99),avg,max' \
k6 run -o experimental-prometheus-rw \
  --tag testid=challenge-read-100vu \
  k6/challenge-read.js
```

## 미션 완료 쓰기

이 테스트는 미션 완료 이력·뱃지·챌린지 집계를 실제로 갱신한다. 조회 테스트와 분리해 실행한다.

```sh
K6_USER_ID_START=1169561 \
K6_MAX_VUS=20 \
K6_PROMETHEUS_RW_SERVER_URL=http://localhost:9090/api/v1/write \
K6_PROMETHEUS_RW_TREND_STATS='p(95),p(99),avg,max' \
k6 run -o experimental-prometheus-rw \
  --tag testid=mission-write-20vu \
  k6/mission-complete.js
```

Grafana는 `http://localhost:3000`, Prometheus는 `http://localhost:9090`에서 확인한다.

## 대시보드 조회

`k6-load-000001`~`001000`은 캐시플로우 예측이 적재된 최악 조건 사용자다.

```sh
K6_DASHBOARD_USER_ID_START=1169561 \
K6_MAX_VUS=50 \
K6_PROMETHEUS_RW_SERVER_URL=http://localhost:9090/api/v1/write \
K6_PROMETHEUS_RW_TREND_STATS='p(95),p(99),avg,max' \
k6 run -o experimental-prometheus-rw \
  --tag testid=dashboard-read-50vu \
  k6/dashboard-read.js
```

## CODEF 제외 혼합 조회

챌린지 80%, 대시보드 20% 비율의 읽기 시나리오다. CODEF 실연동 API는 호출하지 않는다.

```sh
K6_USER_ID_START=1169561 \
K6_MAX_VUS=100 \
K6_PROMETHEUS_RW_SERVER_URL=http://localhost:9090/api/v1/write \
K6_PROMETHEUS_RW_TREND_STATS='p(95),p(99),avg,max' \
k6 run -o experimental-prometheus-rw \
  --tag testid=core-mixed-100vu \
  k6/mixed-core-traffic.js
```

## 한 번에 실행

아래 명령은 `challenge`, `dashboard`, `mixed`, `mission`, `all-read` 중 하나를 실행한다.
`all-read`는 읽기 테스트 3개를 순서대로 실행하며, `mission`만 DB 쓰기 작업이다.

```sh
bash k6/run-no-codef-suite.sh all-read
```

## 결과 분리 기록

실행마다 `K6_TEST_ID`를 다르게 지정하면 Grafana/Prometheus에서 결과가 섞이지 않는다.

```sh
K6_TEST_ID=challenge-10vu-20260822 K6_MAX_VUS=10 bash k6/run-no-codef-suite.sh challenge
K6_TEST_ID=challenge-30vu-20260822 K6_MAX_VUS=30 bash k6/run-no-codef-suite.sh challenge
K6_TEST_ID=challenge-50vu-20260822 K6_MAX_VUS=50 bash k6/run-no-codef-suite.sh challenge
K6_TEST_ID=challenge-100vu-20260822 K6_MAX_VUS=100 bash k6/run-no-codef-suite.sh challenge
```

`K6_MAX_VUS`를 지정하면 시나리오 기본 VU 수보다 우선 적용된다.
