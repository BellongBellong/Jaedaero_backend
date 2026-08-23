-- 노션 운영 기준에 맞춘 챌린지 미션 마스터 데이터입니다.
-- 운영 기준: 데일리 2개, 성향별 추천 후보 4개씩, 1회 미션 1개, 이벤트 미션 2개.
-- 실행 전 20260813_add_mission_code.sql 마이그레이션이 적용되어 있어야 합니다.
-- 기존 완료 이력은 보존하고, 운영 목록 밖의 미션은 비활성화합니다.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET time_zone = '+09:00';
START TRANSACTION;

CREATE TEMPORARY TABLE expected_mission (
    mission_code     VARCHAR(50) COLLATE utf8mb4_unicode_ci NOT NULL,
    mission_type     VARCHAR(10) COLLATE utf8mb4_unicode_ci NULL,
    mission_category VARCHAR(20) COLLATE utf8mb4_unicode_ci NOT NULL,
    title            VARCHAR(255) COLLATE utf8mb4_unicode_ci NOT NULL,
    description      TEXT COLLATE utf8mb4_unicode_ci NULL,
    action_type      VARCHAR(50) COLLATE utf8mb4_unicode_ci NOT NULL,
    display_order    INT NOT NULL,
    trigger_type     VARCHAR(30) COLLATE utf8mb4_unicode_ci NOT NULL,
    trigger_value    INT NULL,
    event_priority   INT NOT NULL,

    PRIMARY KEY (mission_code)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

INSERT INTO expected_mission (
    mission_code,
    mission_type,
    mission_category,
    title,
    description,
    action_type,
    display_order,
    trigger_type,
    trigger_value,
    event_priority
)
VALUES
    ('DAILY_MARKET_REPORT', NULL, 'DAILY', '오늘의 시장 리포트 보기', '오늘의 AI 시장 리포트를 확인해보세요.', 'VIEW_MARKET_REPORT', 1, 'NONE', NULL, 0),
    ('DAILY_TRANSACTION_HISTORY', NULL, 'DAILY', '오늘의 거래 내역 확인하기', '오늘 발생한 거래 내역을 확인해보세요.', 'VIEW_TRANSACTION_HISTORY', 2, 'NONE', NULL, 0),
    ('SAFE_INVESTMENT_GUIDANCE', 'SAFE', 'RECOMMENDED', '적립식 투자 가이드 확인하기', '나에게 맞는 적립식 투자 가이드를 확인해보세요.', 'VIEW_REBALANCING', 1, 'NONE', NULL, 0),
    ('SAFE_MONTHLY_SPENDING_PATTERN', 'SAFE', 'RECOMMENDED', '이번 달 소비 패턴 점검하기', '이번 달 소비 내역을 확인하고 소비 패턴을 점검해보세요.', 'VIEW_SPENDING_ANALYSIS', 2, 'NONE', NULL, 0),
    ('SAFE_SOLDIER_SAVINGS_STATUS', 'SAFE', 'RECOMMENDED', '장병내일준비적금 현황 확인하기', '장병내일준비적금의 납입 현황을 확인해보세요.', 'VIEW_ASSET_OVERVIEW', 3, 'NONE', NULL, 0),
    ('SAFE_DISCHARGE_GOAL_PROGRESS', 'SAFE', 'RECOMMENDED', '전역 목표 달성률 확인하기', '현재 자산과 전역 목표 달성률을 확인해보세요.', 'VIEW_ASSET_STATUS', 4, 'NONE', NULL, 0),
    ('AGGRESSIVE_REBALANCING', 'AGGRESSIVE', 'RECOMMENDED', '투자 추천 확인하기', 'AI가 제안한 투자 추천 내용을 확인해보세요.', 'VIEW_DEPOSIT_PRODUCT', 1, 'NONE', NULL, 0),
    ('AGGRESSIVE_INVESTMENT_RETURN', 'AGGRESSIVE', 'RECOMMENDED', '투자 자산 수익률 확인하기', '투자 자산의 현재 수익률을 확인해보세요.', 'VIEW_ASSET_OVERVIEW', 2, 'NONE', NULL, 0),
    ('AGGRESSIVE_AI_INVESTMENT_ANALYSIS', 'AGGRESSIVE', 'RECOMMENDED', 'AI 투자 분석 받아보기', 'AI 투자 분석을 받아 자산 전략을 점검해보세요.', 'VIEW_DEPOSIT_PRODUCT', 3, 'NONE', NULL, 0),
    ('AGGRESSIVE_WHAT_IF_COMPARISON', 'AGGRESSIVE', 'RECOMMENDED', 'What-if로 투자 금액 비교하기', 'What-if로 투자 금액별 자산 변화를 비교해보세요.', 'RUN_WHAT_IF_SIMULATION', 4, 'NONE', NULL, 0),
    ('SAFE_WHAT_IF_SIMULATION', 'SAFE', 'ONE_TIME', 'What-if 시뮬레이션 하기', '시뮬레이션으로 자산 변화 목표를 설정해보세요.', 'RUN_WHAT_IF_SIMULATION', 1, 'NONE', NULL, 0),
    ('EVENT_PAYDAY_ASSET_ALLOCATION', NULL, 'EVENT', '월급날 자산 배분 해보기', '월급을 저축·투자·소비 목표에 맞춰 배분해보세요.', 'RUN_WHAT_IF_SIMULATION', 1, 'PAYDAY', 10, 1),
    ('EVENT_LEAVE_BENEFIT_CHECK', NULL, 'EVENT', '휴가 혜택 챙기기', '휴가 혜택을 열어 나에게 맞는 혜택을 하나 이상 확인해보세요.', 'VIEW_LEAVE_BENEFIT', 2, 'LEAVE_SCHEDULED', NULL, 2);

-- 코드를 기준으로 수정하여 노출 순서 변경이 기존 미션 ID의 의미를 바꾸지 않게 합니다.
UPDATE mission m
INNER JOIN expected_mission e
    ON m.mission_code = e.mission_code
SET m.mission_type = e.mission_type,
    m.mission_category = e.mission_category,
    m.title = e.title,
    m.description = e.description,
    m.action_type = e.action_type,
    m.display_order = e.display_order,
    m.trigger_type = e.trigger_type,
    m.trigger_value = e.trigger_value,
    m.event_priority = e.event_priority,
    m.is_active = TRUE;

-- 운영 미션이 없는 DB에서도 같은 식별 코드로 누락분만 생성합니다.
INSERT INTO mission (
    mission_code,
    mission_type,
    mission_category,
    title,
    description,
    action_type,
    display_order,
    trigger_type,
    trigger_value,
    event_priority,
    is_active
)
SELECT
    e.mission_code,
    e.mission_type,
    e.mission_category,
    e.title,
    e.description,
    e.action_type,
    e.display_order,
    e.trigger_type,
    e.trigger_value,
    e.event_priority,
    TRUE
FROM expected_mission e
LEFT JOIN mission m
    ON m.mission_code = e.mission_code
WHERE m.mission_id IS NULL;

-- 운영 목록에 없는 기존 미션은 완료 이력을 남긴 채 노출만 중지합니다.
UPDATE mission m
LEFT JOIN expected_mission e
    ON m.mission_code = e.mission_code
SET m.is_active = FALSE
WHERE e.mission_code IS NULL
  AND m.is_active = TRUE;

DROP TEMPORARY TABLE expected_mission;
COMMIT;
