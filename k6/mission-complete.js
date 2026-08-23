import http from 'k6/http';
import { check, sleep } from 'k6';
import execution from 'k6/execution';

const baseUrl = __ENV.BASE_URL || 'http://127.0.0.1:8080';
const userIdStart = Number(__ENV.K6_USER_ID_START || '1169561');
const userCount = Number(__ENV.K6_USER_COUNT || '10000');
const maxVus = Number(__ENV.K6_MAX_VUS || '20');
const sleepSeconds = Number(__ENV.K6_SLEEP_SECONDS || '1');

export const options = {
  scenarios: {
    mission_complete_write: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: __ENV.K6_RAMP_UP || '30s', target: maxVus },
        { duration: __ENV.K6_HOLD || '1m', target: maxVus },
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

function currentUserId() {
  // 시나리오 전체에서 반복 번호를 공유해, 같은 사용자를 동시에 재사용하지 않는다.
  const offset = execution.scenario.iterationInTest % userCount;
  return userIdStart + offset;
}

export default function () {
  const headers = { 'X-User-Id': String(currentUserId()) };
  const todayResponse = http.get(`${baseUrl}/api/v1/missions/today`, {
    headers,
    tags: { name: 'GET /api/v1/missions/today' },
  });

  const todayAvailable = check(todayResponse, {
    'today missions status is 200': (result) => result.status === 200,
  });
  if (!todayAvailable) return;

  const mission = todayResponse.json().find((item) => !item.completed);
  const missionAvailable = check(mission, {
    'an incomplete mission is available': (item) => item && item.missionId > 0,
  });
  if (!missionAvailable) return;

  const completeResponse = http.post(
    `${baseUrl}/api/v1/missions/${mission.missionId}/complete`,
    null,
    {
      headers,
      tags: { name: 'POST /api/v1/missions/{missionId}/complete' },
    },
  );

  check(completeResponse, {
    'mission completion status is 200': (result) => result.status === 200,
  });

  sleep(sleepSeconds);
}
