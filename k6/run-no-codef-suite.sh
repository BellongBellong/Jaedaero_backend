#!/usr/bin/env bash
set -euo pipefail

# CODEF 실연동을 전혀 호출하지 않는 로컬 k6 실행 도우미입니다.
# transaction API는 누락 기간에 CODEF 동기화를 수행할 수 있어 포함하지 않습니다.

scenario="${1:-all-read}"
base_url="${BASE_URL:-http://127.0.0.1:8080}"
user_id_start="${K6_USER_ID_START:-1169561}"
prometheus_url="${K6_PROMETHEUS_RW_SERVER_URL:-http://localhost:9090/api/v1/write}"

run_test() {
  local default_test_id="$1"
  local script_path="$2"
  local default_max_vus="$3"
  local max_vus="${K6_MAX_VUS:-$default_max_vus}"
  local test_id="${K6_TEST_ID:-$default_test_id}"

  BASE_URL="$base_url" \
  K6_USER_ID_START="$user_id_start" \
  K6_DASHBOARD_USER_ID_START="$user_id_start" \
  K6_MAX_VUS="$max_vus" \
  K6_PROMETHEUS_RW_SERVER_URL="$prometheus_url" \
  K6_PROMETHEUS_RW_TREND_STATS='p(95),p(99),avg,max' \
  k6 run -o experimental-prometheus-rw --tag "testid=$test_id" "$script_path"
}

case "$scenario" in
  challenge)
    run_test 'challenge-read-100vu' 'k6/challenge-read.js' 100
    ;;
  dashboard)
    run_test 'dashboard-read-50vu' 'k6/dashboard-read.js' 50
    ;;
  mixed)
    run_test 'core-mixed-no-codef-100vu' 'k6/mixed-core-traffic.js' 100
    ;;
  mission)
    run_test 'mission-write-20vu' 'k6/mission-complete.js' 20
    ;;
  all-read)
    run_test 'challenge-read-100vu' 'k6/challenge-read.js' 100
    run_test 'dashboard-read-50vu' 'k6/dashboard-read.js' 50
    run_test 'core-mixed-no-codef-100vu' 'k6/mixed-core-traffic.js' 100
    ;;
  *)
    echo "Usage: bash k6/run-no-codef-suite.sh {challenge|dashboard|mixed|mission|all-read}" >&2
    exit 2
    ;;
esac
