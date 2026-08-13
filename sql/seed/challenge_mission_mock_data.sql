-- 노션 운영 기준에 맞춘 챌린지 미션 마스터 데이터입니다.
-- 운영 기준: 데일리 2개, 성향별 추천 2개, 1회 미션 1개, 이벤트 미션 1개.
-- 실행: mysql -u <DB_USER> -p jaedaero < sql/seed/challenge_mission_mock_data.sql
-- 기존 완료 이력은 보존하고, 운영 목록 밖의 미션은 비활성화합니다.

SET NAMES utf8mb4;
START TRANSACTION;

CREATE TEMPORARY TABLE expected_mission (
    mission_type   VARCHAR(10) COLLATE utf8mb4_unicode_ci NULL,
    mission_category VARCHAR(20) COLLATE utf8mb4_unicode_ci NOT NULL,
    title          VARCHAR(255) COLLATE utf8mb4_unicode_ci NOT NULL,
    description    TEXT COLLATE utf8mb4_unicode_ci NULL,
    action_type    VARCHAR(50) COLLATE utf8mb4_unicode_ci NOT NULL,
    display_order  INT NOT NULL,
    trigger_type   VARCHAR(30) COLLATE utf8mb4_unicode_ci NOT NULL,
    trigger_value  INT NULL,
    event_priority INT NOT NULL
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

INSERT INTO expected_mission (
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
    (NULL, 'DAILY', '오늘의 시장 리포트 보기', '오늘의 AI 시장 리포트를 확인해보세요.', 'VIEW_MARKET_REPORT', 1, 'NONE', NULL, 0),
    (NULL, 'DAILY', '오늘의 거래 내역 확인하기', '오늘 발생한 거래 내역을 확인해보세요.', 'VIEW_TRANSACTION_HISTORY', 2, 'NONE', NULL, 0),
    ('SAFE', 'RECOMMENDED', '예금상품 살펴보기', '나에게 맞는 예금 상품을 확인해보세요.', 'VIEW_DEPOSIT_PRODUCT', 1, 'NONE', NULL, 0),
    ('AGGRESSIVE', 'RECOMMENDED', '투자 추천 확인하기', 'AI가 제안한 투자 추천 내용을 확인해보세요.', 'VIEW_REBALANCING', 1, 'NONE', NULL, 0),
    ('SAFE', 'ONE_TIME', 'What-if 시뮬레이션 하기', '시뮬레이션으로 자산 변화 목표를 설정해보세요.', 'RUN_WHAT_IF_SIMULATION', 1, 'NONE', NULL, 0),
    (NULL, 'EVENT', '월급날 자산 배분 해보기', '월급을 저축·투자·소비 목표에 맞춰 배분해보세요.', 'RUN_WHAT_IF_SIMULATION', 1, 'PAYDAY', 10, 1);

-- 운영 목록에 없는 기존 미션은 완료 이력을 남긴 채 노출만 중지합니다.
UPDATE mission
SET is_active = FALSE;

UPDATE mission m
INNER JOIN expected_mission e
    ON m.mission_category = e.mission_category
   AND (m.mission_type = e.mission_type
        OR (m.mission_type IS NULL AND e.mission_type IS NULL))
   AND m.display_order = e.display_order
SET m.title = e.title,
    m.description = e.description,
    m.action_type = e.action_type,
    m.trigger_type = e.trigger_type,
    m.trigger_value = e.trigger_value,
    m.event_priority = e.event_priority,
    m.is_active = TRUE;

-- 운영 미션이 없는 DB에서도 동일한 시드 파일로 누락분을 생성합니다.
INSERT INTO mission (
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
    ON m.mission_category = e.mission_category
   AND (m.mission_type = e.mission_type
        OR (m.mission_type IS NULL AND e.mission_type IS NULL))
   AND m.display_order = e.display_order
WHERE m.mission_id IS NULL;

DROP TEMPORARY TABLE expected_mission;
COMMIT;
