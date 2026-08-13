-- 미션 노출 순서와 무관한 영구 식별 코드를 추가합니다.
-- 기존 완료 이력의 mission_id는 변경하거나 이동하지 않습니다.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET time_zone = '+09:00';

SET @mission_code_column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'mission'
      AND column_name = 'mission_code'
);
SET @add_mission_code_column_sql := IF(
    @mission_code_column_exists = 0,
    'ALTER TABLE mission ADD COLUMN mission_code VARCHAR(50) NULL COMMENT ''변경되지 않는 미션 식별 코드'' AFTER mission_id',
    'DO 0'
);
PREPARE add_mission_code_column_statement FROM @add_mission_code_column_sql;
EXECUTE add_mission_code_column_statement;
DEALLOCATE PREPARE add_mission_code_column_statement;

START TRANSACTION;

CREATE TEMPORARY TABLE expected_mission_code (
    mission_code     VARCHAR(50) COLLATE utf8mb4_unicode_ci NOT NULL,
    mission_type     VARCHAR(10) COLLATE utf8mb4_unicode_ci NULL,
    mission_category VARCHAR(20) COLLATE utf8mb4_unicode_ci NOT NULL,
    title            VARCHAR(255) COLLATE utf8mb4_unicode_ci NOT NULL,
    action_type      VARCHAR(50) COLLATE utf8mb4_unicode_ci NOT NULL,
    trigger_type     VARCHAR(30) COLLATE utf8mb4_unicode_ci NOT NULL,

    PRIMARY KEY (mission_code)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

INSERT INTO expected_mission_code (
    mission_code,
    mission_type,
    mission_category,
    title,
    action_type,
    trigger_type
)
VALUES
    ('DAILY_MARKET_REPORT', NULL, 'DAILY', '오늘의 시장 리포트 보기', 'VIEW_MARKET_REPORT', 'NONE'),
    ('DAILY_TRANSACTION_HISTORY', NULL, 'DAILY', '오늘의 거래 내역 확인하기', 'VIEW_TRANSACTION_HISTORY', 'NONE'),
    ('SAFE_DEPOSIT_PRODUCT', 'SAFE', 'RECOMMENDED', '예금상품 살펴보기', 'VIEW_DEPOSIT_PRODUCT', 'NONE'),
    ('AGGRESSIVE_REBALANCING', 'AGGRESSIVE', 'RECOMMENDED', '투자 추천 확인하기', 'VIEW_REBALANCING', 'NONE'),
    ('SAFE_WHAT_IF_SIMULATION', 'SAFE', 'ONE_TIME', 'What-if 시뮬레이션 하기', 'RUN_WHAT_IF_SIMULATION', 'NONE'),
    ('EVENT_PAYDAY_ASSET_ALLOCATION', NULL, 'EVENT', '월급날 자산 배분 해보기', 'RUN_WHAT_IF_SIMULATION', 'PAYDAY');

-- 같은 의미의 중복 행이 있으면 완료 이력이 가장 많은 행을 기존 미션으로 채택합니다.
CREATE TEMPORARY TABLE mission_code_backfill (
    mission_code VARCHAR(50) COLLATE utf8mb4_unicode_ci NOT NULL,
    mission_id   BIGINT NOT NULL,

    PRIMARY KEY (mission_code),
    UNIQUE KEY uq_mission_code_backfill_id (mission_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

INSERT INTO mission_code_backfill (mission_code, mission_id)
SELECT ranked.mission_code, ranked.mission_id
FROM (
    SELECT
        e.mission_code,
        m.mission_id,
        ROW_NUMBER() OVER (
            PARTITION BY e.mission_code
            ORDER BY COUNT(umc.completion_id) DESC, m.is_active DESC, m.mission_id ASC
        ) AS candidate_rank
    FROM expected_mission_code e
    INNER JOIN mission m
        ON m.mission_category = e.mission_category
       AND m.mission_type <=> e.mission_type
       AND m.title = e.title
       AND m.action_type = e.action_type
       AND m.trigger_type = e.trigger_type
       AND m.mission_code IS NULL
    LEFT JOIN user_mission_completion umc
        ON umc.mission_id = m.mission_id
    WHERE NOT EXISTS (
        SELECT 1
        FROM mission coded
        WHERE coded.mission_code = e.mission_code
    )
    GROUP BY e.mission_code, m.mission_id, m.is_active
) ranked
WHERE ranked.candidate_rank = 1;

UPDATE mission m
INNER JOIN mission_code_backfill b
    ON b.mission_id = m.mission_id
SET m.mission_code = b.mission_code
WHERE m.mission_code IS NULL;

-- 같은 의미의 중복 미션에만 있던 완료 이력을 대표 미션에도 복제합니다.
-- 원본 이력과 mission_id는 그대로 보존하며, 이미 존재하는 완료 이력은 건너뜁니다.
INSERT IGNORE INTO user_mission_completion (
    user_id,
    mission_id,
    completion_date,
    completed_at
)
SELECT
    completion.user_id,
    canonical.mission_id,
    completion.completion_date,
    completion.completed_at
FROM expected_mission_code expected
INNER JOIN mission canonical
    ON canonical.mission_code = expected.mission_code
INNER JOIN mission duplicate
    ON duplicate.mission_category = expected.mission_category
   AND duplicate.mission_type <=> expected.mission_type
   AND duplicate.title = expected.title
   AND duplicate.action_type = expected.action_type
   AND duplicate.trigger_type = expected.trigger_type
   AND duplicate.mission_id <> canonical.mission_id
INNER JOIN user_mission_completion completion
    ON completion.mission_id = duplicate.mission_id;

DROP TEMPORARY TABLE mission_code_backfill;
DROP TEMPORARY TABLE expected_mission_code;

COMMIT;

SET @mission_code_unique_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'mission'
      AND index_name = 'uq_mission_code'
      AND non_unique = 0
);
SET @add_mission_code_unique_sql := IF(
    @mission_code_unique_exists = 0,
    'ALTER TABLE mission ADD CONSTRAINT uq_mission_code UNIQUE (mission_code)',
    'DO 0'
);
PREPARE add_mission_code_unique_statement FROM @add_mission_code_unique_sql;
EXECUTE add_mission_code_unique_statement;
DEALLOCATE PREPARE add_mission_code_unique_statement;
