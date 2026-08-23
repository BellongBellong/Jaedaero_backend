import http from 'k6/http';
import { check, sleep } from 'k6';

const baseUrl = __ENV.BASE_URL || 'http://127.0.0.1:8080';
const userIdStart = Number(__ENV.K6_USER_ID_START || '1169561');
const userCount = Number(__ENV.K6_USER_COUNT || '10000');
const maxVus = Number(__ENV.K6_MAX_VUS || '100');
const sleepSeconds = Number(__ENV.K6_SLEEP_SECONDS || '0.2');

export const options = {
  scenarios: {
    challenge_group_read: {
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
    http_req_duration: ['p(95)<1000', 'p(99)<2000'],
  },
};

function currentUserId() {
  // 10,000명 규모의 동일 입대월 그룹을 순환해 랭킹 조회 부하를 재현한다.
  const offset = ((__VU - 1) * 7919 + __ITER) % userCount;
  return userIdStart + offset;
}

export default function () {
  const response = http.get(`${baseUrl}/api/v1/challenges/group?period=CUMULATIVE`, {
    headers: { 'X-User-Id': String(currentUserId()) },
    tags: { name: 'GET /api/v1/challenges/group' },
  });

  check(response, {
    'challenge group status is 200': (result) => result.status === 200,
    'challenge group has ranking payload': (result) => {
      if (result.status !== 200) return false;
      const body = result.json();
      return body.memberCount > 0 && Array.isArray(body.topRankers);
    },
  });

  sleep(sleepSeconds);
}
