import http from 'k6/http';
import { check, sleep } from 'k6';

const baseUrl = __ENV.BASE_URL || 'http://127.0.0.1:8080';
const userIdStart = Number(__ENV.K6_DASHBOARD_USER_ID_START || '1169561');
const userCount = Number(__ENV.K6_DASHBOARD_USER_COUNT || '1000');
const maxVus = Number(__ENV.K6_MAX_VUS || '50');
const sleepSeconds = Number(__ENV.K6_SLEEP_SECONDS || '0.3');

export const options = {
  scenarios: {
    dashboard_read: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: __ENV.K6_RAMP_UP || '30s', target: maxVus },
        { duration: __ENV.K6_HOLD || '2m', target: maxVus },
        { duration: __ENV.K6_RAMP_DOWN || '30s', target: 0 },
      ],
      gracefulRampDown: '10s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<1500', 'p(99)<3000'],
  },
};

export default function () {
  const userId = userIdStart + (((__VU - 1) * 7919 + __ITER) % userCount);
  const response = http.get(`${baseUrl}/api/v1/dashboard`, {
    headers: { 'X-User-Id': String(userId) },
    tags: { name: 'GET /api/v1/dashboard' },
  });

  check(response, {
    'dashboard status is 200': (result) => result.status === 200,
    'dashboard has expected asset': (result) => {
      if (result.status !== 200) return false;
      return result.json().expectedAsset !== undefined;
    },
  });

  sleep(sleepSeconds);
}
