import http from 'k6/http';
import { check, sleep } from 'k6';
import execution from 'k6/execution';

const baseUrl = __ENV.BASE_URL || 'http://127.0.0.1:8080';
const userIdStart = Number(__ENV.K6_USER_ID_START || '1169561');
const maxVus = Number(__ENV.K6_MAX_VUS || '100');
const userCount = Number(__ENV.K6_USER_COUNT || '10000');
const sleepSeconds = Number(__ENV.K6_SLEEP_SECONDS || '0.2');

export const options = {
  scenarios: {
    core_user_traffic: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: __ENV.K6_RAMP_UP || '1m', target: maxVus },
        { duration: __ENV.K6_HOLD || '3m', target: maxVus },
        { duration: __ENV.K6_RAMP_DOWN || '1m', target: 0 },
      ],
      gracefulRampDown: '15s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<1500', 'p(99)<3000'],
  },
};

function userId(count, salt) {
  return userIdStart + (((__VU - 1) * 7919 + __ITER + salt) % count);
}

function request(url, userIdValue, name) {
  const response = http.get(url, {
    headers: { 'X-User-Id': String(userIdValue) },
    tags: { name },
  });
  check(response, { [`${name} status is 200`]: (result) => result.status === 200 });
}

function completeMission() {
  const headers = { 'X-User-Id': String(userIdStart + (execution.scenario.iterationInTest % userCount)) };
  const todayResponse = http.get(`${baseUrl}/api/v1/missions/today`, {
    headers,
    tags: { name: 'GET /api/v1/missions/today' },
  });
  if (!check(todayResponse, { 'mixed today missions status is 200': (result) => result.status === 200 })) return;

  const mission = todayResponse.json().find((item) => !item.completed);
  if (!check(mission, { 'mixed incomplete mission is available': (item) => item && item.missionId > 0 })) return;

  const completeResponse = http.post(`${baseUrl}/api/v1/missions/${mission.missionId}/complete`, null, {
    headers,
    tags: { name: 'POST /api/v1/missions/{missionId}/complete' },
  });
  check(completeResponse, { 'mixed mission completion status is 200': (result) => result.status === 200 });
}

export default function () {
  const traffic = Math.random();

  // 거래내역 API는 누락 기간에 대해 CODEF 동기화를 수행할 수 있으므로 이 시나리오에서는 제외한다.
  if (traffic < 0.75) {
    request(`${baseUrl}/api/v1/challenges/group?period=CUMULATIVE`, userId(10000, 0), 'GET /api/v1/challenges/group');
  } else if (traffic < 0.95) {
    request(`${baseUrl}/api/v1/dashboard`, userId(1000, 0), 'GET /api/v1/dashboard');
  } else {
    completeMission();
  }

  sleep(sleepSeconds);
}
