-- k6 성능 테스트 전용 대용량 데이터 (MySQL 8.0+)
-- 선행 실행: jaedaero_db_v1.sql, badge_policy.sql, challenge_mission_mock_data.sql
-- 기본 규모: 전체 500,000명 / 금융 10,000명 / 최악 조건 1,000명
--
-- 주의:
--   1. 개발·성능 테스트 전용 DB에서 mysql CLI로 실행합니다.
--      실행 계정에는 CREATE ROUTINE, ALTER ROUTINE, EXECUTE 권한이 필요합니다.
--   2. 약 4천만 건 이상을 생성하므로 충분한 디스크와 실행 시간을 확보합니다.
--   3. 재실행하려면 아래 @k6_reset_existing을 1로 바꿉니다. 기존 k6-load-* 사용자는 CASCADE 삭제됩니다.
--   4. 대용량 undo/redo 폭증을 피하기 위해 미션·거래 이력은 배치별로 커밋합니다.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET character_set_connection = utf8mb4;
SET collation_connection = utf8mb4_unicode_ci;
SET time_zone = '+09:00';
SET autocommit = 1;

SET @k6_total_users := COALESCE(@k6_total_users, 500000);
SET @k6_finance_users := COALESCE(@k6_finance_users, 10000);
SET @k6_worst_users := COALESCE(@k6_worst_users, 1000);
SET @k6_batch_size := COALESCE(@k6_batch_size, 10000);
SET @k6_finance_batch_size := COALESCE(@k6_finance_batch_size, 500);
SET @k6_reset_existing := COALESCE(@k6_reset_existing, 0);
SET @k6_special_cohort_date := DATE_SUB(
    STR_TO_DATE(DATE_FORMAT(CURDATE(), '%Y-%m-01'), '%Y-%m-%d'),
    INTERVAL 12 MONTH
);

DROP TEMPORARY TABLE IF EXISTS k6_seed_assert;
CREATE TEMPORARY TABLE k6_seed_assert (
    value INT NOT NULL
);

-- 설정값과 선행 마스터 데이터를 먼저 검증합니다.
INSERT INTO k6_seed_assert (value)
SELECT NULL
WHERE @k6_total_users NOT BETWEEN 450000 AND 500000
   OR @k6_finance_users NOT BETWEEN 10000 AND 50000
   OR @k6_finance_users > @k6_total_users
   OR @k6_worst_users <> 1000
   OR @k6_worst_users > @k6_finance_users
   OR @k6_batch_size < 1000
   OR @k6_finance_batch_size < 100
   OR (
       MOD(@k6_total_users - 18000, 1000) <> 0
       AND MOD(@k6_total_users - 18000, 1000) < 300
   );

INSERT INTO k6_seed_assert (value)
SELECT NULL
WHERE NOT EXISTS (SELECT 1 FROM mission WHERE is_active = TRUE)
   OR NOT EXISTS (SELECT 1 FROM badge WHERE is_active = TRUE);

INSERT INTO k6_seed_assert (value)
SELECT NULL
WHERE @k6_reset_existing = 0
  AND EXISTS (
      SELECT 1
      FROM users
      WHERE social_type = 'KAKAO'
        AND social_id LIKE 'k6-load-%'
  );

-- 기존 k6 데이터는 거래·미션 이력까지 연쇄 삭제되므로 한 번에 삭제하면 InnoDB 락 한도를 넘을 수 있습니다.
-- 100명 단위로 커밋해 성능 테스트 DB를 안전하게 초기화합니다.
DELIMITER $$
DROP PROCEDURE IF EXISTS k6_reset_existing_users$$
CREATE PROCEDURE k6_reset_existing_users()
BEGIN
    DECLARE deleted_rows INT DEFAULT 1;

    WHILE @k6_reset_existing = 1 AND deleted_rows > 0 DO
        DELETE FROM users
        WHERE social_type = 'KAKAO'
          AND social_id LIKE 'k6-load-%'
        LIMIT 100;

        SET deleted_rows = ROW_COUNT();
        COMMIT;
    END WHILE;
END$$
DELIMITER ;

CALL k6_reset_existing_users();
DROP PROCEDURE k6_reset_existing_users;

-- 재귀 CTE 제한 없이 1~500,000과 1~120 수열을 만듭니다.
DROP TEMPORARY TABLE IF EXISTS k6_number;
CREATE TEMPORARY TABLE k6_number (
    n INT NOT NULL PRIMARY KEY
) ENGINE = InnoDB;

INSERT INTO k6_number (n)
WITH digits AS (
    SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
)
SELECT
    ones.n
        + tens.n * 10
        + hundreds.n * 100
        + thousands.n * 1000
        + ten_thousands.n * 10000
        + hundred_thousands.n * 100000
        + 1
FROM digits ones
         CROSS JOIN digits tens
         CROSS JOIN digits hundreds
         CROSS JOIN digits thousands
         CROSS JOIN digits ten_thousands
         CROSS JOIN digits hundred_thousands
WHERE ones.n
          + tens.n * 10
          + hundreds.n * 100
          + thousands.n * 1000
          + ten_thousands.n * 10000
          + hundred_thousands.n * 100000 < @k6_total_users;

DROP TEMPORARY TABLE IF EXISTS k6_small_number;
CREATE TEMPORARY TABLE k6_small_number (
    n SMALLINT NOT NULL PRIMARY KEY
);

INSERT INTO k6_small_number (n)
WITH digits AS (
    SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
)
SELECT ones.n + tens.n * 10 + hundreds.n * 100 + 1
FROM digits ones
         CROSS JOIN digits tens
         CROSS JOIN digits hundreds
WHERE ones.n + tens.n * 10 + hundreds.n * 100 < 120;

-- 10,000 / 5,000 / 3,000명 특수 그룹 뒤에 일반 1,000명 그룹을 배치합니다.
DROP TEMPORARY TABLE IF EXISTS k6_seed_user;
CREATE TEMPORARY TABLE k6_seed_user (
    user_no INT NOT NULL PRIMARY KEY,
    user_id BIGINT NULL,
    group_no INT NOT NULL,
    soldier_type ENUM('ARMY', 'NAVY', 'AIRFORCE', 'MARINE') NOT NULL,
    cohort_year SMALLINT NOT NULL,
    cohort_month TINYINT NOT NULL,
    total_mission_count SMALLINT NOT NULL,
    safe_count SMALLINT NOT NULL,
    aggressive_count SMALLINT NOT NULL,
    initial_preference ENUM('SAFE', 'BALANCED', 'AGGRESSIVE') NOT NULL,
    general_balance BIGINT NOT NULL,
    finance_target BOOLEAN NOT NULL,
    worst_target BOOLEAN NOT NULL,

    INDEX idx_k6_seed_user_user_id (user_id),
    INDEX idx_k6_seed_user_group (soldier_type, cohort_year, cohort_month),
    INDEX idx_k6_seed_user_finance (finance_target, user_no)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

INSERT INTO k6_seed_user (
    user_no,
    group_no,
    soldier_type,
    cohort_year,
    cohort_month,
    total_mission_count,
    safe_count,
    aggressive_count,
    initial_preference,
    general_balance,
    finance_target,
    worst_target
)
WITH user_group_plan AS (
    SELECT
        sequence_no.n AS user_no,
        CASE
            WHEN sequence_no.n <= 10000 THEN 0
            WHEN sequence_no.n <= 15000 THEN 1
            WHEN sequence_no.n <= 18000 THEN 2
            ELSE 3 + FLOOR((sequence_no.n - 18001) / 1000)
        END AS group_no,
        30 + MOD(sequence_no.n * 17, 71) AS total_mission_count
    FROM k6_number sequence_no
), cohort_plan AS (
    SELECT
        plan.*,
        CASE plan.group_no
            WHEN 0 THEN _utf8mb4'ARMY'
            WHEN 1 THEN _utf8mb4'NAVY'
            WHEN 2 THEN _utf8mb4'AIRFORCE'
            ELSE ELT(
                MOD(plan.group_no - 3, 4) + 1,
                _utf8mb4'ARMY', _utf8mb4'NAVY', _utf8mb4'AIRFORCE', _utf8mb4'MARINE'
            )
        END AS soldier_type,
        CASE
            WHEN plan.group_no <= 2 THEN CAST(@k6_special_cohort_date AS DATE)
            ELSE DATE_ADD(
                CAST(_utf8mb4'2010-01-01' AS DATE),
                INTERVAL FLOOR((plan.group_no - 3) / 4) MONTH
            )
        END AS cohort_date
    FROM user_group_plan plan
)
SELECT
    cohort.user_no,
    cohort.group_no,
    cohort.soldier_type,
    YEAR(cohort.cohort_date),
    MONTH(cohort.cohort_date),
    cohort.total_mission_count,
    CEILING(cohort.total_mission_count / 2),
    FLOOR(cohort.total_mission_count / 2),
    ELT(
        MOD(cohort.user_no - 1, 3) + 1,
        _utf8mb4'SAFE', _utf8mb4'BALANCED', _utf8mb4'AGGRESSIVE'
    ),
    1000000 + MOD(cohort.user_no * 7919, 3000000),
    cohort.user_no <= @k6_finance_users,
    cohort.user_no <= @k6_worst_users
FROM cohort_plan cohort;

-- 동일 코호트에 기존 비-k6 사용자가 있으면 특수 그룹 크기가 변하므로 중단합니다.
INSERT INTO k6_seed_assert (value)
SELECT NULL
WHERE EXISTS (
    SELECT 1
    FROM (
        SELECT DISTINCT soldier_type, cohort_year, cohort_month
        FROM k6_seed_user
    ) planned_group
    INNER JOIN challenge_group challenge
        ON challenge.soldier_type = planned_group.soldier_type
       AND challenge.enlistment_year = planned_group.cohort_year
       AND challenge.enlistment_month = planned_group.cohort_month
    INNER JOIN challenge_member member ON member.group_id = challenge.group_id
    INNER JOIN users user_account ON user_account.user_id = member.user_id
    WHERE user_account.social_id NOT LIKE 'k6-load-%'
);

-- 전체 사용자·프로필·목표·챌린지 기반 데이터
INSERT INTO users (
    social_type,
    social_id,
    nickname,
    profile_image,
    profile_source,
    is_withdrawn,
    created_at
)
SELECT
    'KAKAO',
    CONCAT('k6-load-', LPAD(seed.user_no, 6, '0')),
    CONCAT('k6u', LPAD(seed.user_no, 6, '0')),
    seed.soldier_type,
    ELT(
        MOD(seed.user_no - 1, 6) + 1,
        _utf8mb4'GREEN', _utf8mb4'OLIVE', _utf8mb4'YELLOW',
        _utf8mb4'ORANGE', _utf8mb4'GRAY', _utf8mb4'BLACK'
    ),
    FALSE,
    TIMESTAMP(STR_TO_DATE(CONCAT(seed.cohort_year, '-', LPAD(seed.cohort_month, 2, '0'), '-01'), '%Y-%m-%d'))
FROM k6_seed_user seed;

UPDATE k6_seed_user seed
INNER JOIN users user_account
    ON user_account.social_type = 'KAKAO'
   AND user_account.social_id = CONCAT('k6-load-', LPAD(seed.user_no, 6, '0'))
SET seed.user_id = user_account.user_id;

INSERT INTO soldier_profile (
    user_id,
    soldier_type,
    rank_name,
    enlistment_date,
    discharge_date,
    saving_join_yn
)
SELECT
    seed.user_id,
    seed.soldier_type,
    ELT(
        MOD(seed.user_no - 1, 4) + 1,
        _utf8mb4'이병', _utf8mb4'일병', _utf8mb4'상병', _utf8mb4'병장'
    ),
    STR_TO_DATE(CONCAT(seed.cohort_year, '-', LPAD(seed.cohort_month, 2, '0'), '-01'), '%Y-%m-%d'),
    DATE_ADD(
        STR_TO_DATE(CONCAT(seed.cohort_year, '-', LPAD(seed.cohort_month, 2, '0'), '-01'), '%Y-%m-%d'),
        INTERVAL 18 MONTH
    ),
    seed.finance_target
FROM k6_seed_user seed;

INSERT INTO goal (user_id, target_amount, target_date, status)
SELECT
    seed.user_id,
    20000000 + MOD(seed.user_no, 21) * 1000000,
    profile.discharge_date,
    'ACTIVE'
FROM k6_seed_user seed
INNER JOIN soldier_profile profile ON profile.user_id = seed.user_id;

INSERT INTO challenge_group (soldier_type, enlistment_year, enlistment_month)
SELECT DISTINCT seed.soldier_type, seed.cohort_year, seed.cohort_month
FROM k6_seed_user seed
ON DUPLICATE KEY UPDATE soldier_type = VALUES(soldier_type);

INSERT INTO challenge_member (group_id, user_id, joined_at)
SELECT
    challenge.group_id,
    seed.user_id,
    TIMESTAMP(STR_TO_DATE(CONCAT(seed.cohort_year, '-', LPAD(seed.cohort_month, 2, '0'), '-01'), '%Y-%m-%d'))
FROM k6_seed_user seed
INNER JOIN challenge_group challenge
    ON challenge.soldier_type = seed.soldier_type
   AND challenge.enlistment_year = seed.cohort_year
   AND challenge.enlistment_month = seed.cohort_month;

INSERT INTO challenge_member_summary (member_id, total_mission_count)
SELECT member.member_id, seed.total_mission_count
FROM k6_seed_user seed
INNER JOIN challenge_member member ON member.user_id = seed.user_id;

INSERT INTO investment_badge (
    user_id,
    initial_preference,
    badge_tier,
    safe_count,
    safe_grade,
    aggressive_count,
    aggressive_grade
)
SELECT
    seed.user_id,
    seed.initial_preference,
    seed.initial_preference,
    seed.safe_count,
    CASE
        WHEN seed.safe_count >= 50 THEN _utf8mb4'GOLD'
        WHEN seed.safe_count >= 10 THEN _utf8mb4'SILVER'
        ELSE _utf8mb4'BRONZE'
    END,
    seed.aggressive_count,
    CASE
        WHEN seed.aggressive_count >= 50 THEN _utf8mb4'GOLD'
        WHEN seed.aggressive_count >= 10 THEN _utf8mb4'SILVER'
        ELSE _utf8mb4'BRONZE'
    END
FROM k6_seed_user seed;

INSERT INTO user_badge (user_id, badge_id, acquired_at)
SELECT
    seed.user_id,
    badge_policy.badge_id,
    DATE_SUB(CURRENT_TIMESTAMP, INTERVAL badge_policy.required_completion_count DAY)
FROM k6_seed_user seed
INNER JOIN badge badge_policy
    ON badge_policy.is_active = TRUE
   AND badge_policy.required_completion_count <= CASE badge_policy.mission_type
       WHEN _utf8mb4'SAFE' THEN seed.safe_count
       ELSE seed.aggressive_count
   END;

DROP TEMPORARY TABLE IF EXISTS k6_active_mission;
CREATE TEMPORARY TABLE k6_active_mission (
    mission_no SMALLINT NOT NULL PRIMARY KEY,
    mission_id BIGINT NOT NULL UNIQUE
);

INSERT INTO k6_active_mission (mission_no, mission_id)
SELECT
    ROW_NUMBER() OVER (ORDER BY mission_category, display_order, mission_id),
    mission_id
FROM mission
WHERE is_active = TRUE;

SET @k6_mission_catalog_size := (SELECT COUNT(*) FROM k6_active_mission);

-- ponytail: 10k 사용자 커밋은 undo/redo 상한을 위한 값입니다. DB 용량에 맞게 @k6_batch_size만 조정합니다.
DELIMITER $$
DROP PROCEDURE IF EXISTS k6_seed_challenge_history_batches$$
CREATE PROCEDURE k6_seed_challenge_history_batches()
BEGIN
    DECLARE batch_start INT DEFAULT 1;
    DECLARE batch_end INT;

    WHILE batch_start <= @k6_total_users DO
        SET batch_end = LEAST(batch_start + @k6_batch_size - 1, @k6_total_users);

        INSERT INTO challenge_monthly_result (
            member_id,
            result_month,
            mission_completion_count
        )
        SELECT
            member.member_id,
            DATE_SUB(
                STR_TO_DATE(DATE_FORMAT(CURDATE(), '%Y-%m-01'), '%Y-%m-%d'),
                INTERVAL month_sequence.n - 1 MONTH
            ),
            FLOOR(seed.total_mission_count / 6)
                + IF(month_sequence.n <= MOD(seed.total_mission_count, 6), 1, 0)
        FROM k6_seed_user seed
        INNER JOIN challenge_member member ON member.user_id = seed.user_id
        INNER JOIN k6_small_number month_sequence ON month_sequence.n <= 6
        WHERE seed.user_no BETWEEN batch_start AND batch_end;

        INSERT INTO user_mission_completion (
            user_id,
            mission_id,
            completion_date,
            completed_at
        )
        SELECT
            seed.user_id,
            active_mission.mission_id,
            DATE_SUB(CURDATE(), INTERVAL completion_sequence.n DAY),
            DATE_ADD(
                DATE_SUB(CURDATE(), INTERVAL completion_sequence.n DAY),
                INTERVAL 36000 + MOD(seed.user_no, 36000) SECOND
            )
        FROM k6_seed_user seed
        INNER JOIN k6_small_number completion_sequence
            ON completion_sequence.n <= seed.total_mission_count
        INNER JOIN k6_active_mission active_mission
            ON active_mission.mission_no = MOD(completion_sequence.n - 1, @k6_mission_catalog_size) + 1
        WHERE seed.user_no BETWEEN batch_start AND batch_end;

        COMMIT;
        SET batch_start = batch_end + 1;
    END WHILE;
END$$
DELIMITER ;

CALL k6_seed_challenge_history_batches();
DROP PROCEDURE k6_seed_challenge_history_batches;

-- 금융 대상자: 입출금 1개 + 군 적금 2개
INSERT INTO codef_connection (
    user_id,
    connected_id_encrypted,
    connected_id_hash,
    status,
    last_sync_at
)
SELECT
    seed.user_id,
    CONCAT('k6-encrypted-connection-', seed.user_no),
    SHA2(CONCAT('k6-connection-', seed.user_no), 256),
    'ACTIVE',
    CURRENT_TIMESTAMP
FROM k6_seed_user seed
WHERE seed.finance_target = TRUE;

DROP TEMPORARY TABLE IF EXISTS k6_account_plan;
CREATE TEMPORARY TABLE k6_account_plan (
    account_no TINYINT NOT NULL PRIMARY KEY,
    institution_code VARCHAR(20) NOT NULL,
    institution_name VARCHAR(100) NOT NULL,
    account_type VARCHAR(50) NOT NULL,
    account_role ENUM('SOLDIER_SAVING', 'NARASARANG', 'GENERAL') NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    monthly_amount BIGINT NULL,
    interest_rate DECIMAL(5, 2) NULL
) DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

INSERT INTO k6_account_plan (
    account_no,
    institution_code,
    institution_name,
    account_type,
    account_role,
    product_name,
    monthly_amount,
    interest_rate
)
VALUES
    (1, '004', 'KB국민은행', 'DEMAND_DEPOSIT', 'GENERAL', 'k6 입출금 통장', NULL, NULL),
    (2, '004', 'KB국민은행', 'INSTALLMENT_SAVINGS', 'SOLDIER_SAVING', 'KB 장병내일준비적금', 250000, 5.00),
    (3, '088', '신한은행', 'INSTALLMENT_SAVINGS', 'SOLDIER_SAVING', '신한 장병내일준비적금', 300000, 5.50);

INSERT INTO connected_account (
    connection_id,
    institution_code,
    institution_name,
    account_number_encrypted,
    account_number_hash,
    account_masked,
    business_type,
    account_type,
    account_role,
    product_name,
    current_balance,
    available_balance,
    account_opened_date,
    maturity_date,
    last_synced_at,
    status
)
SELECT
    connection_info.connection_id,
    plan.institution_code,
    plan.institution_name,
    CONCAT('k6-encrypted-account-', seed.user_no, '-', plan.account_no),
    SHA2(CONCAT('k6-account-', seed.user_no, '-', plan.account_no), 256),
    CONCAT('K6-', LPAD(seed.user_no, 6, '0'), '-', plan.account_no),
    'BK',
    plan.account_type,
    plan.account_role,
    plan.product_name,
    CASE plan.account_no
        WHEN 1 THEN seed.general_balance
        ELSE plan.monthly_amount * 6
    END,
    CASE plan.account_no
        WHEN 1 THEN seed.general_balance
        ELSE NULL
    END,
    DATE_SUB(CURDATE(), INTERVAL 5 MONTH),
    CASE
        WHEN plan.account_role = _utf8mb4'SOLDIER_SAVING' THEN DATE_ADD(CURDATE(), INTERVAL 13 MONTH)
        ELSE NULL
    END,
    CURRENT_TIMESTAMP,
    'ACTIVE'
FROM k6_seed_user seed
INNER JOIN codef_connection connection_info ON connection_info.user_id = seed.user_id
CROSS JOIN k6_account_plan plan
WHERE seed.finance_target = TRUE;

INSERT INTO soldier_saving (
    user_id,
    account_id,
    bank_name,
    monthly_amount,
    interest_rate,
    government_support_expected,
    start_date,
    end_date
)
SELECT
    seed.user_id,
    account.account_id,
    account.institution_name,
    plan.monthly_amount,
    plan.interest_rate,
    plan.monthly_amount * 6,
    account.account_opened_date,
    account.maturity_date
FROM k6_seed_user seed
INNER JOIN codef_connection connection_info ON connection_info.user_id = seed.user_id
INNER JOIN connected_account account
    ON account.connection_id = connection_info.connection_id
   AND account.account_role = 'SOLDIER_SAVING'
INNER JOIN k6_account_plan plan
    ON plan.account_no IN (2, 3)
   AND account.account_number_hash = SHA2(CONCAT('k6-account-', seed.user_no, '-', plan.account_no), 256)
WHERE seed.finance_target = TRUE;

-- ponytail: 거래는 500명 단위 커밋으로 제한합니다. 더 작은 배치는 왕복만 늘립니다.
DELIMITER $$
DROP PROCEDURE IF EXISTS k6_seed_finance_history_batches$$
CREATE PROCEDURE k6_seed_finance_history_batches()
BEGIN
    DECLARE batch_start INT DEFAULT 1;
    DECLARE batch_end INT;

    WHILE batch_start <= @k6_finance_users DO
        SET batch_end = LEAST(batch_start + @k6_finance_batch_size - 1, @k6_finance_users);

        INSERT INTO transaction_history (
            account_id,
            transaction_datetime,
            amount,
            balance_after,
            transaction_type,
            category,
            category_source,
            transaction_description,
            external_transaction_key
        )
        WITH digits AS (
            SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
            UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
        ), month_sequence AS (
            SELECT n + 1 AS n
            FROM digits
            WHERE n < 6
        ), transaction_sequence AS (
            SELECT ones.n + tens.n * 10 + hundreds.n * 100 + 1 AS n
            FROM digits ones
            CROSS JOIN digits tens
            CROSS JOIN digits hundreds
            WHERE ones.n + tens.n * 10 + hundreds.n * 100 < 120
        )
        SELECT
            account.account_id,
            DATE_ADD(
                DATE_ADD(
                    DATE_SUB(
                        STR_TO_DATE(DATE_FORMAT(CURDATE(), '%Y-%m-01'), '%Y-%m-%d'),
                        INTERVAL month_sequence.n - 1 MONTH
                    ),
                    INTERVAL MOD(transaction_sequence.n - 1, 28) DAY
                ),
                INTERVAL 28800 + MOD(transaction_sequence.n * 977 + seed.user_no, 43200) SECOND
            ),
            CASE
                WHEN MOD(transaction_sequence.n, 10) = 0
                    THEN 900000 + MOD(seed.user_no, 5) * 50000
                ELSE 1000 + MOD(seed.user_no * transaction_sequence.n * 97, 49000)
            END,
            GREATEST(
                0,
                seed.general_balance
                    + IF(MOD(transaction_sequence.n, 10) = 0, 900000, -1000 * transaction_sequence.n)
            ),
            IF(
                MOD(transaction_sequence.n, 10) = 0,
                _utf8mb4'DEPOSIT',
                _utf8mb4'WITHDRAW'
            ),
            CASE MOD(transaction_sequence.n, 10)
                WHEN 0 THEN _utf8mb4'SALARY'
                WHEN 1 THEN _utf8mb4'FOOD'
                WHEN 2 THEN _utf8mb4'TRANSPORT'
                WHEN 3 THEN _utf8mb4'SHOPPING'
                ELSE _utf8mb4'ETC'
            END,
            _utf8mb4'RULE',
            IF(
                MOD(transaction_sequence.n, 10) = 0,
                _utf8mb4'월급',
                CONCAT(_utf8mb4'체크카드 k6 부하거래 ', transaction_sequence.n)
            ),
            SHA2(
                CONCAT('k6-general-', seed.user_no, '-', month_sequence.n, '-', transaction_sequence.n),
                256
            )
        FROM k6_seed_user seed
        INNER JOIN codef_connection connection_info ON connection_info.user_id = seed.user_id
        INNER JOIN connected_account account
            ON account.connection_id = connection_info.connection_id
           AND account.account_type = 'DEMAND_DEPOSIT'
        INNER JOIN month_sequence ON TRUE
        INNER JOIN transaction_sequence
            ON transaction_sequence.n <= CASE
                WHEN seed.worst_target = TRUE THEN 120
                ELSE 30 + MOD(seed.user_no * 13, 71)
            END
        WHERE seed.user_no BETWEEN batch_start AND batch_end
          AND seed.finance_target = TRUE;

        INSERT INTO transaction_history (
            account_id,
            transaction_datetime,
            amount,
            balance_after,
            transaction_type,
            category,
            category_source,
            transaction_description,
            external_transaction_key
        )
        WITH month_sequence AS (
            SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3
            UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6
        )
        SELECT
            account.account_id,
            DATE_ADD(
                DATE_SUB(
                    STR_TO_DATE(DATE_FORMAT(CURDATE(), '%Y-%m-01'), '%Y-%m-%d'),
                    INTERVAL month_sequence.n - 1 MONTH
                ),
                INTERVAL 4 DAY
            ),
            saving.monthly_amount,
            saving.monthly_amount * (7 - month_sequence.n),
            'DEPOSIT',
            'ASSET',
            'RULE',
            '장병내일준비적금 납입',
            SHA2(
                CONCAT('k6-saving-', account.account_id, '-', DATE_FORMAT(
                    DATE_SUB(
                        STR_TO_DATE(DATE_FORMAT(CURDATE(), '%Y-%m-01'), '%Y-%m-%d'),
                        INTERVAL month_sequence.n - 1 MONTH
                    ),
                    '%Y-%m'
                )),
                256
            )
        FROM k6_seed_user seed
        INNER JOIN soldier_saving saving ON saving.user_id = seed.user_id
        INNER JOIN connected_account account ON account.account_id = saving.account_id
        INNER JOIN month_sequence ON TRUE
        WHERE seed.user_no BETWEEN batch_start AND batch_end
          AND seed.finance_target = TRUE;

        COMMIT;
        SET batch_start = batch_end + 1;
    END WHILE;
END$$
DELIMITER ;

CALL k6_seed_finance_history_batches();
DROP PROCEDURE k6_seed_finance_history_batches;

-- 이미 적재한 수시입출금 거래 기간을 동기화 완료 범위로 기록해 거래 조회 API가 CODEF를 재호출하지 않게 합니다.
INSERT INTO account_transaction_sync (
    account_id,
    inquiry_type,
    requested_start_date,
    requested_end_date
)
SELECT
    account.account_id,
    _utf8mb4'DEMAND_DEPOSIT',
    DATE_SUB(
        STR_TO_DATE(DATE_FORMAT(CURDATE(), '%Y-%m-01'), '%Y-%m-%d'),
        INTERVAL 5 MONTH
    ),
    CURDATE()
FROM k6_seed_user seed
INNER JOIN codef_connection connection_info ON connection_info.user_id = seed.user_id
INNER JOIN connected_account account
    ON account.connection_id = connection_info.connection_id
   AND account.account_type = _utf8mb4'DEMAND_DEPOSIT'
WHERE seed.finance_target = TRUE;

-- 최악 조건 1,000명: 최신 자산 스냅샷과 캐시플로우 예측 1건 + 6개월 상세
INSERT INTO asset_snapshot (
    user_id,
    total_asset,
    total_saving,
    total_spending,
    snapshot_date
)
SELECT
    seed.user_id,
    SUM(account.current_balance),
    SUM(IF(account.account_role = _utf8mb4'SOLDIER_SAVING', account.current_balance, 0)),
    3000000 + MOD(MAX(seed.user_no), 20) * 10000,
    CURDATE()
FROM k6_seed_user seed
INNER JOIN codef_connection connection_info ON connection_info.user_id = seed.user_id
INNER JOIN connected_account account ON account.connection_id = connection_info.connection_id
WHERE seed.worst_target = TRUE
GROUP BY seed.user_id;

INSERT INTO cashflow_forecast (
    user_id,
    base_asset,
    expected_salary,
    expected_spending,
    expected_saving_amount,
    expected_investment_amount,
    soldier_saving_principal,
    investment_principal,
    expected_asset,
    soldier_saving_interest,
    government_matching_support,
    expected_investment_return,
    projected_benefit_amount,
    calculation_policy_version,
    monthly_spending_limit,
    achievement_rate,
    financial_discharge_date,
    policy_version,
    generated_at
)
SELECT
    seed.user_id,
    snapshot.total_asset,
    10800000,
    3600000,
    3300000,
    0,
    snapshot.total_saving + 3300000,
    0,
    snapshot.total_asset + 10500000,
    180000,
    3300000,
    0,
    3480000,
    'K6_LOAD_V1',
    600000,
    LEAST(999.99, ROUND((snapshot.total_asset + 10500000) / goal_info.target_amount * 100, 2)),
    profile.discharge_date,
    'K6-2026',
    CURRENT_TIMESTAMP
FROM k6_seed_user seed
INNER JOIN asset_snapshot snapshot
    ON snapshot.user_id = seed.user_id
   AND snapshot.snapshot_date = CURDATE()
INNER JOIN goal goal_info ON goal_info.user_id = seed.user_id
INNER JOIN soldier_profile profile ON profile.user_id = seed.user_id
WHERE seed.worst_target = TRUE;

INSERT INTO cashflow_forecast_month (
    forecast_id,
    forecast_month,
    expected_rank,
    expected_salary,
    expected_saving_amount,
    expected_investment_amount,
    expected_spending_amount,
    expected_ending_asset
)
SELECT
    forecast.forecast_id,
    DATE_ADD(
        STR_TO_DATE(DATE_FORMAT(CURDATE(), '%Y-%m-01'), '%Y-%m-%d'),
        INTERVAL month_sequence.n - 1 MONTH
    ),
    profile.rank_name,
    900000,
    550000,
    0,
    600000,
    forecast.base_asset + month_sequence.n * 850000
FROM k6_seed_user seed
INNER JOIN cashflow_forecast forecast
    ON forecast.user_id = seed.user_id
   AND forecast.calculation_policy_version = 'K6_LOAD_V1'
INNER JOIN soldier_profile profile ON profile.user_id = seed.user_id
INNER JOIN k6_small_number month_sequence ON month_sequence.n <= 6
WHERE seed.worst_target = TRUE;

-- 생성 결과 검증. 하나라도 어긋나면 NOT NULL 위반으로 실행을 실패 처리합니다.
INSERT INTO k6_seed_assert (value)
SELECT NULL
WHERE (SELECT COUNT(*) FROM k6_seed_user) <> @k6_total_users;

INSERT INTO k6_seed_assert (value)
SELECT NULL
WHERE (SELECT COUNT(*) FROM users WHERE social_type = 'KAKAO' AND social_id LIKE 'k6-load-%') <> @k6_total_users;

INSERT INTO k6_seed_assert (value)
SELECT NULL
WHERE (
    SELECT COUNT(*)
    FROM soldier_profile profile
    INNER JOIN users user_account ON user_account.user_id = profile.user_id
    WHERE user_account.social_type = 'KAKAO'
      AND user_account.social_id LIKE 'k6-load-%'
) <> @k6_total_users;

INSERT INTO k6_seed_assert (value)
SELECT NULL
WHERE (
    SELECT COUNT(*)
    FROM goal goal_info
    INNER JOIN users user_account ON user_account.user_id = goal_info.user_id
    WHERE user_account.social_type = 'KAKAO'
      AND user_account.social_id LIKE 'k6-load-%'
) <> @k6_total_users;

INSERT INTO k6_seed_assert (value)
SELECT NULL
WHERE (
    SELECT COUNT(*)
    FROM challenge_member member
    INNER JOIN users user_account ON user_account.user_id = member.user_id
    WHERE user_account.social_type = 'KAKAO'
      AND user_account.social_id LIKE 'k6-load-%'
) <> @k6_total_users;

INSERT INTO k6_seed_assert (value)
SELECT NULL
WHERE (
    SELECT COUNT(*)
    FROM challenge_member_summary summary
    INNER JOIN challenge_member member ON member.member_id = summary.member_id
    INNER JOIN users user_account ON user_account.user_id = member.user_id
    WHERE user_account.social_type = 'KAKAO'
      AND user_account.social_id LIKE 'k6-load-%'
) <> @k6_total_users;

INSERT INTO k6_seed_assert (value)
SELECT NULL
WHERE EXISTS (
    SELECT
        seed.group_no,
        COUNT(*) AS member_count
    FROM k6_seed_user seed
    INNER JOIN challenge_member member ON member.user_id = seed.user_id
    GROUP BY seed.group_no
    HAVING (seed.group_no = 0 AND member_count <> 10000)
        OR (seed.group_no = 1 AND member_count <> 5000)
        OR (seed.group_no = 2 AND member_count <> 3000)
        OR (seed.group_no >= 3 AND member_count NOT BETWEEN 300 AND 1000)
);

INSERT INTO k6_seed_assert (value)
SELECT NULL
WHERE (
    SELECT COUNT(*)
    FROM challenge_monthly_result monthly_result
    INNER JOIN challenge_member member ON member.member_id = monthly_result.member_id
    INNER JOIN users user_account ON user_account.user_id = member.user_id
    WHERE user_account.social_type = 'KAKAO'
      AND user_account.social_id LIKE 'k6-load-%'
) <> @k6_total_users * 6;

SET @k6_expected_mission_completion := (SELECT SUM(total_mission_count) FROM k6_seed_user);

INSERT INTO k6_seed_assert (value)
SELECT NULL
WHERE (
    SELECT COUNT(*)
    FROM user_mission_completion completion
    INNER JOIN users user_account ON user_account.user_id = completion.user_id
    WHERE user_account.social_type = 'KAKAO'
      AND user_account.social_id LIKE 'k6-load-%'
) <> @k6_expected_mission_completion;

INSERT INTO k6_seed_assert (value)
SELECT NULL
WHERE EXISTS (
    SELECT seed.user_id
    FROM k6_seed_user seed
    INNER JOIN codef_connection connection_info ON connection_info.user_id = seed.user_id
    LEFT JOIN connected_account account ON account.connection_id = connection_info.connection_id
    WHERE seed.finance_target = TRUE
    GROUP BY seed.user_id
    HAVING COUNT(account.account_id) <> 3
        OR SUM(account.account_role = 'SOLDIER_SAVING') <> 2
);

INSERT INTO k6_seed_assert (value)
SELECT NULL
WHERE (
    SELECT COUNT(*)
    FROM soldier_saving saving
    INNER JOIN k6_seed_user seed ON seed.user_id = saving.user_id
    WHERE seed.finance_target = TRUE
) <> @k6_finance_users * 2;

INSERT INTO k6_seed_assert (value)
SELECT NULL
WHERE (
    SELECT COUNT(*)
    FROM asset_snapshot snapshot
    INNER JOIN k6_seed_user seed ON seed.user_id = snapshot.user_id
    WHERE seed.worst_target = TRUE
) <> @k6_worst_users;

INSERT INTO k6_seed_assert (value)
SELECT NULL
WHERE (
    SELECT COUNT(*)
    FROM cashflow_forecast forecast
    INNER JOIN k6_seed_user seed ON seed.user_id = forecast.user_id
    WHERE seed.worst_target = TRUE
      AND forecast.calculation_policy_version = 'K6_LOAD_V1'
) <> @k6_worst_users;

INSERT INTO k6_seed_assert (value)
SELECT NULL
WHERE (
    SELECT COUNT(*)
    FROM cashflow_forecast_month forecast_month
    INNER JOIN cashflow_forecast forecast ON forecast.forecast_id = forecast_month.forecast_id
    INNER JOIN k6_seed_user seed ON seed.user_id = forecast.user_id
    WHERE seed.worst_target = TRUE
      AND forecast.calculation_policy_version = 'K6_LOAD_V1'
) <> @k6_worst_users * 6;

-- k6 실행 준비 확인용 요약과 대표 사용자 ID
SELECT 'users' AS dataset, @k6_total_users AS expected_count, COUNT(*) AS actual_count
FROM users
WHERE social_type = 'KAKAO' AND social_id LIKE 'k6-load-%';

SELECT 'challenge_monthly_result', @k6_total_users * 6, COUNT(*)
FROM challenge_monthly_result monthly_result
INNER JOIN challenge_member member ON member.member_id = monthly_result.member_id
INNER JOIN users user_account ON user_account.user_id = member.user_id
WHERE user_account.social_type = 'KAKAO'
  AND user_account.social_id LIKE 'k6-load-%';

SELECT
    'user_mission_completion',
    @k6_expected_mission_completion,
    COUNT(completion.completion_id)
FROM user_mission_completion completion
INNER JOIN users user_account ON user_account.user_id = completion.user_id
WHERE user_account.social_type = 'KAKAO'
  AND user_account.social_id LIKE 'k6-load-%';

SELECT 'finance_accounts', @k6_finance_users * 3, COUNT(account.account_id)
FROM k6_seed_user seed
INNER JOIN codef_connection connection_info ON connection_info.user_id = seed.user_id
INNER JOIN connected_account account ON account.connection_id = connection_info.connection_id
WHERE seed.finance_target = TRUE;

SELECT 'worst_cashflow_forecast', @k6_worst_users, COUNT(forecast.forecast_id)
FROM k6_seed_user seed
INNER JOIN cashflow_forecast forecast
    ON forecast.user_id = seed.user_id
   AND forecast.calculation_policy_version = 'K6_LOAD_V1'
WHERE seed.worst_target = TRUE;

SELECT
    seed.group_no,
    seed.soldier_type,
    seed.cohort_year,
    seed.cohort_month,
    COUNT(*) AS member_count
FROM k6_seed_user seed
GROUP BY seed.group_no, seed.soldier_type, seed.cohort_year, seed.cohort_month
ORDER BY member_count DESC, seed.group_no
LIMIT 20;

SELECT
    seed.user_no,
    seed.user_id,
    CONCAT('k6-load-', LPAD(seed.user_no, 6, '0')) AS social_id,
    seed.finance_target,
    seed.worst_target
FROM k6_seed_user seed
WHERE seed.user_no <= 20
ORDER BY seed.user_no;

DROP TEMPORARY TABLE k6_account_plan;
DROP TEMPORARY TABLE k6_active_mission;
DROP TEMPORARY TABLE k6_seed_user;
DROP TEMPORARY TABLE k6_small_number;
DROP TEMPORARY TABLE k6_number;
DROP TEMPORARY TABLE k6_seed_assert;
