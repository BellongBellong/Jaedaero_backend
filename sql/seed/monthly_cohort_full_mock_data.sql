-- 2026년 3월~8월 입대 동기 통합 Mock 데이터
-- 선행 실행: badge_policy.sql, challenge_mission_mock_data.sql
-- 군종·월별 10명씩 총 240명과 프로필·목표·챌린지·뱃지·계좌·거래·분석 이력을 생성합니다.
-- 재실행 시 monthly-cohort-* 전용 소셜 식별자의 데이터만 갱신합니다.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET time_zone = '+09:00';
START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS monthly_cohort_service;
CREATE TEMPORARY TABLE monthly_cohort_service (
    service_order TINYINT NOT NULL PRIMARY KEY,
    service_code VARCHAR(20) NOT NULL UNIQUE,
    soldier_type ENUM('ARMY', 'NAVY', 'AIRFORCE', 'MARINE') NOT NULL UNIQUE
);

INSERT INTO monthly_cohort_service (service_order, service_code, soldier_type)
VALUES
    (1, 'army', 'ARMY'),
    (2, 'navy', 'NAVY'),
    (3, 'airforce', 'AIRFORCE'),
    (4, 'marine', 'MARINE');

DROP TEMPORARY TABLE IF EXISTS monthly_cohort_seed_user;
CREATE TEMPORARY TABLE monthly_cohort_seed_user (
    social_id VARCHAR(255) NOT NULL PRIMARY KEY,
    nickname VARCHAR(50) NOT NULL UNIQUE,
    soldier_type ENUM('ARMY', 'NAVY', 'AIRFORCE', 'MARINE') NOT NULL,
    cohort_month TINYINT NOT NULL,
    cohort_rank TINYINT NOT NULL,
    total_mission_count INT NOT NULL,
    safe_count INT NOT NULL,
    aggressive_count INT NOT NULL,
    rank_name VARCHAR(20) NOT NULL,
    profile_source ENUM('GREEN', 'OLIVE', 'YELLOW', 'ORANGE', 'GRAY', 'BLACK') NOT NULL,
    initial_preference ENUM('SAFE', 'BALANCED', 'AGGRESSIVE') NOT NULL,
    general_balance BIGINT NOT NULL,
    saving_balance BIGINT NOT NULL,
    brokerage_balance BIGINT NOT NULL,
    UNIQUE KEY uq_monthly_cohort_service_month_rank (soldier_type, cohort_month, cohort_rank)
) DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

INSERT INTO monthly_cohort_seed_user (
    social_id,
    nickname,
    soldier_type,
    cohort_month,
    cohort_rank,
    total_mission_count,
    safe_count,
    aggressive_count,
    rank_name,
    profile_source,
    initial_preference,
    general_balance,
    saving_balance,
    brokerage_balance
)
WITH RECURSIVE user_sequence AS (
    SELECT 1 AS user_no
    UNION ALL
    SELECT user_no + 1
    FROM user_sequence
    WHERE user_no < 60
), seed_value AS (
    SELECT
        sequence.user_no,
        service.service_order,
        service.service_code,
        service.soldier_type,
        3 + FLOOR((user_no - 1) / 10) AS cohort_month,
        MOD(user_no - 1, 10) + 1 AS cohort_rank
    FROM user_sequence sequence
    CROSS JOIN monthly_cohort_service service
), mission_value AS (
    SELECT
        *,
        (9 - cohort_month) * 10 - cohort_rank + 1 AS total_mission_count
    FROM seed_value
)
SELECT
    CASE
        WHEN soldier_type = 'ARMY' THEN CONCAT(
            'monthly-cohort-2026-',
            LPAD(cohort_month, 2, '0'),
            '-',
            LPAD(cohort_rank, 2, '0')
        )
        ELSE CONCAT(
            'monthly-cohort-',
            service_code,
            '-2026-',
            LPAD(cohort_month, 2, '0'),
            '-',
            LPAD(cohort_rank, 2, '0')
        )
    END,
    CASE service_order
        WHEN 1 THEN ELT(
            user_no,
            '김도윤', '이준서', '박민재', '최현우', '정우진',
            '강지훈', '조승현', '윤재민', '장태윤', '임건우',
            '한시우', '오민준', '서지호', '신도현', '권준영',
            '황성민', '안재현', '송유찬', '전하준', '홍지환',
            '문태민', '양승우', '손정민', '배준혁', '백현준',
            '허진우', '유도훈', '남시현', '심재원', '노건희',
            '곽민성', '성준호', '차동현', '주우성', '우재윤',
            '구민규', '민승민', '진현수', '나준수', '지성훈',
            '김태오', '이로운', '박연우', '최서진', '정민호',
            '강현민', '조은찬', '윤태경', '장도현', '임주원',
            '한재하', '오승찬', '서민석', '신우빈', '권태훈',
            '황준서', '안시윤', '송민찬', '전유준', '홍건호'
        )
        ELSE CONCAT(
            ELT(
                MOD(user_no - 1, 20) + 1,
                '김', '이', '박', '최', '정', '강', '조', '윤', '장', '임',
                '한', '오', '서', '신', '권', '황', '안', '송', '전', '홍'
            ),
            CASE service_order
                WHEN 2 THEN ELT(FLOOR((user_no - 1) / 20) + 1, '하람', '지율', '온유')
                WHEN 3 THEN ELT(FLOOR((user_no - 1) / 20) + 1, '이든', '가온', '다온')
                ELSE ELT(FLOOR((user_no - 1) / 20) + 1, '라온', '해온', '예찬')
            END
        )
    END,
    soldier_type,
    cohort_month,
    cohort_rank,
    total_mission_count,
    CASE MOD(cohort_rank, 3)
        WHEN 1 THEN total_mission_count
        WHEN 2 THEN FLOOR(total_mission_count / 2)
        ELSE FLOOR(total_mission_count / 3)
    END,
    CASE MOD(cohort_rank, 3)
        WHEN 1 THEN FLOOR(total_mission_count / 3)
        WHEN 2 THEN FLOOR(total_mission_count / 2)
        ELSE total_mission_count
    END,
    CASE
        WHEN cohort_month = 3 THEN '상병'
        WHEN cohort_month <= 6 THEN '일병'
        ELSE '이병'
    END,
    ELT(MOD(user_no - 1, 6) + 1, 'GREEN', 'OLIVE', 'YELLOW', 'ORANGE', 'GRAY', 'BLACK'),
    ELT(MOD(cohort_rank - 1, 3) + 1, 'SAFE', 'BALANCED', 'AGGRESSIVE'),
    1000000 + cohort_month * 100000 + cohort_rank * 10000,
    (9 - cohort_month) * 550000,
    500000 + (9 - cohort_month) * 100000 + cohort_rank * 20000
FROM mission_value;

-- 사용자·군 프로필·전역 자산 목표
INSERT INTO users (
    social_type,
    social_id,
    nickname,
    profile_image,
    profile_source,
    is_withdrawn,
    withdrawn_at,
    created_at
)
SELECT
    'KAKAO',
    seed.social_id,
    seed.nickname,
    seed.soldier_type,
    seed.profile_source,
    FALSE,
    NULL,
    TIMESTAMP(DATE_ADD('2026-03-01', INTERVAL seed.cohort_month - 3 MONTH))
FROM monthly_cohort_seed_user seed
ON DUPLICATE KEY UPDATE
    nickname = VALUES(nickname),
    profile_image = VALUES(profile_image),
    profile_source = VALUES(profile_source),
    is_withdrawn = FALSE,
    withdrawn_at = NULL,
    created_at = VALUES(created_at);

INSERT INTO soldier_profile (
    user_id,
    soldier_type,
    rank_name,
    enlistment_date,
    discharge_date,
    saving_join_yn
)
SELECT
    user_account.user_id,
    seed.soldier_type,
    seed.rank_name,
    DATE_ADD('2026-03-01', INTERVAL seed.cohort_month - 3 MONTH),
    DATE_ADD('2026-03-01', INTERVAL seed.cohort_month + 15 MONTH),
    TRUE
FROM monthly_cohort_seed_user seed
INNER JOIN users user_account
    ON user_account.social_type = 'KAKAO'
   AND user_account.social_id = seed.social_id
ON DUPLICATE KEY UPDATE
    soldier_type = VALUES(soldier_type),
    rank_name = VALUES(rank_name),
    enlistment_date = VALUES(enlistment_date),
    discharge_date = VALUES(discharge_date),
    saving_join_yn = TRUE;

INSERT INTO goal (user_id, target_amount, target_date, status)
SELECT
    user_account.user_id,
    CASE seed.soldier_type
        WHEN 'NAVY' THEN
            24000000 + seed.cohort_rank * 1000000
                - IF(seed.cohort_rank >= 6, 1000000, 0)
        WHEN 'AIRFORCE' THEN
            26000000 + seed.cohort_rank * 1000000
                - IF(seed.cohort_rank >= 6, 1000000, 0)
        ELSE 20000000 + seed.cohort_rank * 1000000
    END,
    profile.discharge_date,
    'ACTIVE'
FROM monthly_cohort_seed_user seed
INNER JOIN users user_account
    ON user_account.social_type = 'KAKAO'
   AND user_account.social_id = seed.social_id
INNER JOIN soldier_profile profile ON profile.user_id = user_account.user_id
ON DUPLICATE KEY UPDATE
    target_amount = VALUES(target_amount),
    target_date = VALUES(target_date),
    status = 'ACTIVE';

-- 입대월 동기 챌린지와 미션 완료 이력
INSERT INTO challenge_group (soldier_type, enlistment_year, enlistment_month)
WITH RECURSIVE cohort_month AS (
    SELECT 3 AS month_no
    UNION ALL
    SELECT month_no + 1
    FROM cohort_month
    WHERE month_no < 8
)
SELECT service.soldier_type, 2026, month.month_no
FROM cohort_month month
CROSS JOIN monthly_cohort_service service
WHERE TRUE
ON DUPLICATE KEY UPDATE
    soldier_type = VALUES(soldier_type);

INSERT INTO challenge_member (group_id, user_id, joined_at)
SELECT
    challenge.group_id,
    user_account.user_id,
    TIMESTAMP(DATE_ADD('2026-03-01', INTERVAL seed.cohort_month - 3 MONTH))
FROM monthly_cohort_seed_user seed
INNER JOIN users user_account
    ON user_account.social_type = 'KAKAO'
   AND user_account.social_id = seed.social_id
INNER JOIN challenge_group challenge
    ON challenge.soldier_type = seed.soldier_type
   AND challenge.enlistment_year = 2026
   AND challenge.enlistment_month = seed.cohort_month
ON DUPLICATE KEY UPDATE
    joined_at = VALUES(joined_at);

INSERT INTO challenge_member_summary (member_id, total_mission_count)
SELECT member.member_id, seed.total_mission_count
FROM monthly_cohort_seed_user seed
INNER JOIN users user_account
    ON user_account.social_type = 'KAKAO'
   AND user_account.social_id = seed.social_id
INNER JOIN challenge_member member ON member.user_id = user_account.user_id
INNER JOIN challenge_group challenge
    ON challenge.group_id = member.group_id
   AND challenge.soldier_type = seed.soldier_type
   AND challenge.enlistment_year = 2026
   AND challenge.enlistment_month = seed.cohort_month
ON DUPLICATE KEY UPDATE
    total_mission_count = VALUES(total_mission_count),
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO challenge_monthly_result (
    member_id,
    result_month,
    mission_completion_count
)
SELECT member.member_id, '2026-08-01', 11 - seed.cohort_rank
FROM monthly_cohort_seed_user seed
INNER JOIN users user_account
    ON user_account.social_type = 'KAKAO'
   AND user_account.social_id = seed.social_id
INNER JOIN challenge_member member ON member.user_id = user_account.user_id
INNER JOIN challenge_group challenge
    ON challenge.group_id = member.group_id
   AND challenge.soldier_type = seed.soldier_type
   AND challenge.enlistment_year = 2026
   AND challenge.enlistment_month = seed.cohort_month
ON DUPLICATE KEY UPDATE
    mission_completion_count = VALUES(mission_completion_count);

DROP TEMPORARY TABLE IF EXISTS monthly_cohort_seed_assert;
CREATE TEMPORARY TABLE monthly_cohort_seed_assert (
    value INT NOT NULL
);

INSERT INTO monthly_cohort_seed_assert (value)
SELECT NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM mission
    WHERE is_active = TRUE
);

-- 전용 목 사용자의 완료 이력은 집계값과 정확히 맞추기 위해 다시 구성합니다.
DELETE completion
FROM user_mission_completion completion
INNER JOIN users user_account ON user_account.user_id = completion.user_id
INNER JOIN monthly_cohort_seed_user seed ON seed.social_id = user_account.social_id
WHERE user_account.social_type = 'KAKAO';

INSERT INTO user_mission_completion (user_id, mission_id, completion_date)
WITH RECURSIVE completion_sequence AS (
    SELECT 1 AS completion_no
    UNION ALL
    SELECT completion_no + 1
    FROM completion_sequence
    WHERE completion_no < 60
), ordered_mission AS (
    SELECT
        mission_id,
        ROW_NUMBER() OVER (ORDER BY mission_category, display_order, mission_id) AS mission_no
    FROM mission
    WHERE is_active = TRUE
), mission_total AS (
    SELECT COUNT(*) AS mission_count
    FROM ordered_mission
)
SELECT
    user_account.user_id,
    ordered.mission_id,
    DATE_SUB('2026-08-21', INTERVAL FLOOR((sequence.completion_no - 1) / total.mission_count) DAY)
FROM monthly_cohort_seed_user seed
INNER JOIN users user_account
    ON user_account.social_type = 'KAKAO'
   AND user_account.social_id = seed.social_id
INNER JOIN completion_sequence sequence
    ON sequence.completion_no <= seed.total_mission_count
CROSS JOIN mission_total total
INNER JOIN ordered_mission ordered
    ON ordered.mission_no = MOD(sequence.completion_no - 1, total.mission_count) + 1;

-- 성향별 누적 뱃지와 획득 이력
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
    user_account.user_id,
    seed.initial_preference,
    seed.initial_preference,
    seed.safe_count,
    CASE
        WHEN seed.safe_count >= 50 THEN 'GOLD'
        WHEN seed.safe_count >= 10 THEN 'SILVER'
        WHEN seed.safe_count >= 1 THEN 'BRONZE'
        ELSE NULL
    END,
    seed.aggressive_count,
    CASE
        WHEN seed.aggressive_count >= 50 THEN 'GOLD'
        WHEN seed.aggressive_count >= 10 THEN 'SILVER'
        WHEN seed.aggressive_count >= 1 THEN 'BRONZE'
        ELSE NULL
    END
FROM monthly_cohort_seed_user seed
INNER JOIN users user_account
    ON user_account.social_type = 'KAKAO'
   AND user_account.social_id = seed.social_id
ON DUPLICATE KEY UPDATE
    initial_preference = VALUES(initial_preference),
    badge_tier = VALUES(badge_tier),
    safe_count = VALUES(safe_count),
    safe_grade = VALUES(safe_grade),
    aggressive_count = VALUES(aggressive_count),
    aggressive_grade = VALUES(aggressive_grade);

DELETE user_badge_history
FROM user_badge user_badge_history
INNER JOIN users user_account ON user_account.user_id = user_badge_history.user_id
INNER JOIN monthly_cohort_seed_user seed ON seed.social_id = user_account.social_id
WHERE user_account.social_type = 'KAKAO';

INSERT INTO user_badge (user_id, badge_id, acquired_at)
SELECT
    user_account.user_id,
    badge_policy.badge_id,
    TIMESTAMP(DATE_ADD('2026-03-01', INTERVAL seed.cohort_month - 3 MONTH))
FROM monthly_cohort_seed_user seed
INNER JOIN users user_account
    ON user_account.social_type = 'KAKAO'
   AND user_account.social_id = seed.social_id
INNER JOIN badge badge_policy ON (
    (badge_policy.mission_type = 'SAFE'
        AND badge_policy.required_completion_count <= seed.safe_count)
    OR (badge_policy.mission_type = 'AGGRESSIVE'
        AND badge_policy.required_completion_count <= seed.aggressive_count)
)
WHERE badge_policy.is_active = TRUE;

-- CODEF 목 연결과 입출금·군적금·증권 계좌
INSERT INTO codef_connection (
    user_id,
    connected_id_encrypted,
    connected_id_hash,
    status,
    last_sync_at
)
SELECT
    user_account.user_id,
    CONCAT('mock-encrypted-', seed.social_id),
    SHA2(CONCAT('mock-connected-', seed.social_id), 256),
    'ACTIVE',
    '2026-08-21 09:00:00'
FROM monthly_cohort_seed_user seed
INNER JOIN users user_account
    ON user_account.social_type = 'KAKAO'
   AND user_account.social_id = seed.social_id
ON DUPLICATE KEY UPDATE
    connected_id_encrypted = VALUES(connected_id_encrypted),
    connected_id_hash = VALUES(connected_id_hash),
    status = 'ACTIVE',
    last_sync_at = VALUES(last_sync_at),
    last_sync_error_message = NULL;

DROP TEMPORARY TABLE IF EXISTS monthly_cohort_account_plan;
CREATE TEMPORARY TABLE monthly_cohort_account_plan (
    account_code VARCHAR(20) NOT NULL PRIMARY KEY,
    institution_code VARCHAR(20) NOT NULL,
    institution_name VARCHAR(100) NOT NULL,
    business_type ENUM('BK', 'ST') NOT NULL,
    account_type VARCHAR(50) NOT NULL,
    account_role ENUM('SOLDIER_SAVING', 'NARASARANG', 'GENERAL') NOT NULL,
    product_name VARCHAR(255) NOT NULL
) DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

INSERT INTO monthly_cohort_account_plan (
    account_code,
    institution_code,
    institution_name,
    business_type,
    account_type,
    account_role,
    product_name
)
VALUES
    ('NARASARANG', '004', 'KB국민은행', 'BK', 'DEMAND_DEPOSIT', 'NARASARANG', '나라사랑 우대통장'),
    ('SOLDIER_SAVING', '088', '신한은행', 'BK', 'INSTALLMENT_SAVINGS', 'SOLDIER_SAVING', '장병내일준비적금'),
    ('BROKERAGE', '0309', '미래에셋증권', 'ST', 'BROKERAGE', 'GENERAL', '종합매매계좌');

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
    CONCAT('mock-encrypted-', seed.social_id, '-', LOWER(plan.account_code)),
    SHA2(CONCAT('mock-account-', seed.social_id, '-', LOWER(plan.account_code)), 256),
    CONCAT(
        '26',
        LPAD(seed.cohort_month, 2, '0'),
        '-',
        LPAD(seed.cohort_rank, 2, '0'),
        '-',
        RIGHT(plan.account_code, 4)
    ),
    plan.business_type,
    plan.account_type,
    plan.account_role,
    plan.product_name,
    CASE plan.account_code
        WHEN 'NARASARANG' THEN seed.general_balance
        WHEN 'SOLDIER_SAVING' THEN seed.saving_balance
        ELSE seed.brokerage_balance
    END,
    CASE plan.account_code
        WHEN 'SOLDIER_SAVING' THEN NULL
        ELSE CASE plan.account_code
            WHEN 'NARASARANG' THEN seed.general_balance
            ELSE seed.brokerage_balance
        END
    END,
    DATE_ADD('2026-03-01', INTERVAL seed.cohort_month - 3 MONTH),
    CASE
        WHEN plan.account_code = 'SOLDIER_SAVING'
            THEN DATE_ADD('2026-03-01', INTERVAL seed.cohort_month + 15 MONTH)
        ELSE NULL
    END,
    '2026-08-21 09:00:00',
    'ACTIVE'
FROM monthly_cohort_seed_user seed
INNER JOIN users user_account
    ON user_account.social_type = 'KAKAO'
   AND user_account.social_id = seed.social_id
INNER JOIN codef_connection connection_info ON connection_info.user_id = user_account.user_id
CROSS JOIN monthly_cohort_account_plan plan
WHERE TRUE
ON DUPLICATE KEY UPDATE
    institution_name = VALUES(institution_name),
    account_masked = VALUES(account_masked),
    business_type = VALUES(business_type),
    account_type = VALUES(account_type),
    account_role = VALUES(account_role),
    product_name = VALUES(product_name),
    current_balance = VALUES(current_balance),
    available_balance = VALUES(available_balance),
    account_opened_date = VALUES(account_opened_date),
    maturity_date = VALUES(maturity_date),
    last_synced_at = VALUES(last_synced_at),
    status = 'ACTIVE';

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
    user_account.user_id,
    account.account_id,
    account.institution_name,
    550000,
    5.00,
    seed.saving_balance,
    account.account_opened_date,
    account.maturity_date
FROM monthly_cohort_seed_user seed
INNER JOIN users user_account
    ON user_account.social_type = 'KAKAO'
   AND user_account.social_id = seed.social_id
INNER JOIN codef_connection connection_info ON connection_info.user_id = user_account.user_id
INNER JOIN connected_account account
    ON account.connection_id = connection_info.connection_id
   AND account.account_role = 'SOLDIER_SAVING'
   AND account.account_number_hash = SHA2(
       CONCAT('mock-account-', seed.social_id, '-soldier_saving'),
       256
   )
ON DUPLICATE KEY UPDATE
    monthly_amount = VALUES(monthly_amount),
    interest_rate = VALUES(interest_rate),
    government_support_expected = VALUES(government_support_expected),
    start_date = VALUES(start_date),
    end_date = VALUES(end_date);

INSERT INTO recurring_investment_plan (
    user_id,
    brokerage_account_id,
    frequency,
    contribution_day,
    contribution_amount,
    maximum_monthly_amount,
    investment_product_code,
    investment_product_name,
    status,
    next_contribution_date
)
SELECT
    user_account.user_id,
    account.account_id,
    'MONTHLY',
    10,
    100000 + seed.cohort_rank * 10000,
    300000,
    '069500',
    'KODEX 200',
    'ACTIVE',
    '2026-09-10'
FROM monthly_cohort_seed_user seed
INNER JOIN users user_account
    ON user_account.social_type = 'KAKAO'
   AND user_account.social_id = seed.social_id
INNER JOIN codef_connection connection_info ON connection_info.user_id = user_account.user_id
INNER JOIN connected_account account
    ON account.connection_id = connection_info.connection_id
   AND account.business_type = 'ST'
   AND account.account_number_hash = SHA2(
       CONCAT('mock-account-', seed.social_id, '-brokerage'),
       256
   )
ON DUPLICATE KEY UPDATE
    brokerage_account_id = VALUES(brokerage_account_id),
    frequency = VALUES(frequency),
    contribution_day = VALUES(contribution_day),
    contribution_amount = VALUES(contribution_amount),
    maximum_monthly_amount = VALUES(maximum_monthly_amount),
    investment_product_code = VALUES(investment_product_code),
    investment_product_name = VALUES(investment_product_name),
    status = VALUES(status),
    next_contribution_date = VALUES(next_contribution_date);

DROP TEMPORARY TABLE IF EXISTS monthly_cohort_general_transaction;
CREATE TEMPORARY TABLE monthly_cohort_general_transaction (
    transaction_suffix VARCHAR(20) NOT NULL PRIMARY KEY,
    transaction_datetime DATETIME NOT NULL,
    amount BIGINT NOT NULL,
    balance_offset BIGINT NOT NULL,
    transaction_type ENUM('DEPOSIT', 'WITHDRAW') NOT NULL,
    category VARCHAR(50) NULL,
    category_source ENUM('RULE', 'AI', 'USER') NULL,
    transaction_description VARCHAR(255) NOT NULL
) DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

INSERT INTO monthly_cohort_general_transaction (
    transaction_suffix,
    transaction_datetime,
    amount,
    balance_offset,
    transaction_type,
    category,
    category_source,
    transaction_description
)
VALUES
    ('salary', '2026-08-10 09:00:00', 900000, 630000, 'DEPOSIT', 'SALARY', 'RULE', '월급'),
    ('food', '2026-08-11 12:10:00', 25000, 605000, 'WITHDRAW', 'FOOD', 'RULE', '체크카드 식비'),
    ('transport', '2026-08-12 18:20:00', 15000, 590000, 'WITHDRAW', 'TRANSPORT', 'RULE', '교통카드 충전'),
    ('shopping', '2026-08-13 20:10:00', 40000, 550000, 'WITHDRAW', 'SHOPPING', 'AI', '온라인 쇼핑'),
    ('saving-transfer', '2026-08-14 09:00:00', 550000, 0, 'WITHDRAW', 'ASSET', 'USER', '장병내일준비적금 이체');

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
SELECT
    account.account_id,
    template.transaction_datetime,
    template.amount,
    seed.general_balance + template.balance_offset,
    template.transaction_type,
    template.category,
    template.category_source,
    template.transaction_description,
    SHA2(CONCAT(seed.social_id, '-general-', template.transaction_suffix), 256)
FROM monthly_cohort_seed_user seed
INNER JOIN users user_account
    ON user_account.social_type = 'KAKAO'
   AND user_account.social_id = seed.social_id
INNER JOIN codef_connection connection_info ON connection_info.user_id = user_account.user_id
INNER JOIN connected_account account
    ON account.connection_id = connection_info.connection_id
   AND account.account_number_hash = SHA2(
       CONCAT('mock-account-', seed.social_id, '-narasarang'),
       256
   )
CROSS JOIN monthly_cohort_general_transaction template
WHERE TRUE
ON DUPLICATE KEY UPDATE
    transaction_datetime = VALUES(transaction_datetime),
    amount = VALUES(amount),
    balance_after = VALUES(balance_after),
    transaction_type = VALUES(transaction_type),
    category = VALUES(category),
    category_source = VALUES(category_source),
    transaction_description = VALUES(transaction_description);

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
WITH RECURSIVE saving_month AS (
    SELECT 3 AS month_no
    UNION ALL
    SELECT month_no + 1
    FROM saving_month
    WHERE month_no < 8
)
SELECT
    account.account_id,
    STR_TO_DATE(
        CONCAT('2026-', LPAD(month.month_no, 2, '0'), '-05 09:00:00'),
        '%Y-%m-%d %H:%i:%s'
    ),
    550000,
    (month.month_no - seed.cohort_month + 1) * 550000,
    'DEPOSIT',
    NULL,
    NULL,
    CONCAT('장병내일준비적금 ', month.month_no, '월 납입'),
    SHA2(CONCAT(seed.social_id, '-saving-2026-', LPAD(month.month_no, 2, '0')), 256)
FROM monthly_cohort_seed_user seed
INNER JOIN users user_account
    ON user_account.social_type = 'KAKAO'
   AND user_account.social_id = seed.social_id
INNER JOIN codef_connection connection_info ON connection_info.user_id = user_account.user_id
INNER JOIN connected_account account
    ON account.connection_id = connection_info.connection_id
   AND account.account_number_hash = SHA2(
       CONCAT('mock-account-', seed.social_id, '-soldier_saving'),
       256
   )
INNER JOIN saving_month month ON month.month_no >= seed.cohort_month
ON DUPLICATE KEY UPDATE
    transaction_datetime = VALUES(transaction_datetime),
    amount = VALUES(amount),
    balance_after = VALUES(balance_after),
    transaction_description = VALUES(transaction_description);

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
SELECT
    account.account_id,
    CASE transaction_template.sequence_no
        WHEN 1 THEN '2026-08-16 09:00:00'
        ELSE '2026-08-17 10:00:00'
    END,
    CASE transaction_template.sequence_no
        WHEN 1 THEN seed.brokerage_balance + 200000
        ELSE 200000
    END,
    CASE transaction_template.sequence_no
        WHEN 1 THEN seed.brokerage_balance + 200000
        ELSE seed.brokerage_balance
    END,
    CASE transaction_template.sequence_no
        WHEN 1 THEN 'DEPOSIT'
        ELSE 'WITHDRAW'
    END,
    'ASSET',
    'RULE',
    CASE transaction_template.sequence_no
        WHEN 1 THEN '증권계좌 투자금 입금'
        ELSE 'KODEX 200 매수'
    END,
    SHA2(CONCAT(seed.social_id, '-brokerage-', transaction_template.sequence_no), 256)
FROM monthly_cohort_seed_user seed
INNER JOIN users user_account
    ON user_account.social_type = 'KAKAO'
   AND user_account.social_id = seed.social_id
INNER JOIN codef_connection connection_info ON connection_info.user_id = user_account.user_id
INNER JOIN connected_account account
    ON account.connection_id = connection_info.connection_id
   AND account.account_number_hash = SHA2(
       CONCAT('mock-account-', seed.social_id, '-brokerage'),
       256
   )
CROSS JOIN (
    SELECT 1 AS sequence_no
    UNION ALL
    SELECT 2
) transaction_template
WHERE TRUE
ON DUPLICATE KEY UPDATE
    transaction_datetime = VALUES(transaction_datetime),
    amount = VALUES(amount),
    balance_after = VALUES(balance_after),
    transaction_type = VALUES(transaction_type),
    category = VALUES(category),
    category_source = VALUES(category_source),
    transaction_description = VALUES(transaction_description);

-- 자산 스냅샷과 What-if·AI 분석 이력
INSERT INTO asset_snapshot (
    user_id,
    total_asset,
    total_saving,
    total_spending,
    snapshot_date
)
SELECT
    user_account.user_id,
    SUM(account.current_balance),
    SUM(CASE WHEN account.account_role = 'SOLDIER_SAVING' THEN account.current_balance ELSE 0 END),
    80000,
    '2026-08-20'
FROM monthly_cohort_seed_user seed
INNER JOIN users user_account
    ON user_account.social_type = 'KAKAO'
   AND user_account.social_id = seed.social_id
INNER JOIN codef_connection connection_info ON connection_info.user_id = user_account.user_id
INNER JOIN connected_account account
    ON account.connection_id = connection_info.connection_id
   AND account.status = 'ACTIVE'
GROUP BY user_account.user_id
ON DUPLICATE KEY UPDATE
    total_asset = VALUES(total_asset),
    total_saving = VALUES(total_saving),
    total_spending = VALUES(total_spending);

INSERT INTO simulation (
    user_id,
    scenario_name,
    target_amount,
    monthly_saving_amount,
    monthly_investment_amount,
    expected_return_rate,
    monthly_spending_amount,
    expected_asset,
    financial_discharge_date,
    is_saved,
    created_at
)
SELECT
    user_account.user_id,
    '월별 동기 기본 시뮬레이션',
    goal_info.target_amount,
    550000,
    100000 + seed.cohort_rank * 10000,
    4.00 + MOD(seed.cohort_rank, 4),
    300000 + seed.cohort_rank * 10000,
    snapshot.total_asset + 18000000 + seed.cohort_rank * 200000,
    profile.discharge_date,
    TRUE,
    TIMESTAMP('2026-08-20 10:00:00') + INTERVAL seed.cohort_rank MINUTE
FROM monthly_cohort_seed_user seed
INNER JOIN users user_account
    ON user_account.social_type = 'KAKAO'
   AND user_account.social_id = seed.social_id
INNER JOIN soldier_profile profile ON profile.user_id = user_account.user_id
INNER JOIN goal goal_info ON goal_info.user_id = user_account.user_id
INNER JOIN asset_snapshot snapshot
    ON snapshot.user_id = user_account.user_id
   AND snapshot.snapshot_date = '2026-08-20'
WHERE NOT EXISTS (
    SELECT 1
    FROM simulation existing_simulation
    WHERE existing_simulation.user_id = user_account.user_id
      AND existing_simulation.scenario_name = '월별 동기 기본 시뮬레이션'
);

INSERT INTO ai_analysis (
    user_id,
    snapshot_id,
    simulation_id,
    analysis_type,
    result_json,
    input_data_hash,
    model_name,
    prompt_version,
    generation_source,
    created_at
)
SELECT
    user_account.user_id,
    snapshot.snapshot_id,
    simulation_history.simulation_id,
    'CONSUMPTION',
    JSON_OBJECT(
        'comment', CONCAT(seed.nickname, '님의 거래 내역을 분석한 결과 저축 흐름이 안정적입니다.'),
        'expectedAsset', simulation_history.expected_asset,
        'spendingPattern', JSON_OBJECT(
            'totalSpendingAmount', 80000,
            'changeAmount', -10000,
            'changeRate', -11.11
        ),
        'spendingExpectedEffect', JSON_OBJECT(
            'expectedAssetIncreaseAmount', 120000,
            'expectedAssetAfterImprovement', simulation_history.expected_asset + 120000
        )
    ),
    SHA2(CONCAT(seed.social_id, '-analysis-2026-08-20'), 256),
    'mock-template',
    'monthly-cohort-v1',
    'FALLBACK',
    TIMESTAMP('2026-08-20 11:00:00') + INTERVAL seed.cohort_rank MINUTE
FROM monthly_cohort_seed_user seed
INNER JOIN users user_account
    ON user_account.social_type = 'KAKAO'
   AND user_account.social_id = seed.social_id
INNER JOIN asset_snapshot snapshot
    ON snapshot.user_id = user_account.user_id
   AND snapshot.snapshot_date = '2026-08-20'
INNER JOIN simulation simulation_history
    ON simulation_history.user_id = user_account.user_id
   AND simulation_history.scenario_name = '월별 동기 기본 시뮬레이션'
WHERE NOT EXISTS (
    SELECT 1
    FROM ai_analysis existing_analysis
    WHERE existing_analysis.user_id = user_account.user_id
      AND existing_analysis.input_data_hash = SHA2(
          CONCAT(seed.social_id, '-analysis-2026-08-20'),
          256
      )
);

UPDATE ai_analysis analysis_history
INNER JOIN users user_account ON user_account.user_id = analysis_history.user_id
INNER JOIN monthly_cohort_seed_user seed ON seed.social_id = user_account.social_id
SET analysis_history.result_json = JSON_SET(
    analysis_history.result_json,
    '$.comment',
    CONCAT(seed.nickname, '님의 거래 내역을 분석한 결과 저축 흐름이 안정적입니다.')
)
WHERE user_account.social_type = 'KAKAO'
  AND analysis_history.prompt_version = 'monthly-cohort-v1';

-- 최소 정합성 검사: 하나라도 어긋나면 NOT NULL 위반으로 전체 트랜잭션을 중단합니다.
INSERT INTO monthly_cohort_seed_assert (value)
SELECT NULL
WHERE (SELECT COUNT(*) FROM monthly_cohort_seed_user) <> 240;

INSERT INTO monthly_cohort_seed_assert (value)
SELECT NULL
WHERE EXISTS (
    SELECT soldier_type, cohort_month
    FROM monthly_cohort_seed_user
    GROUP BY soldier_type, cohort_month
    HAVING COUNT(*) <> 10
);

INSERT INTO monthly_cohort_seed_assert (value)
SELECT NULL
WHERE EXISTS (
    SELECT soldier_type
    FROM monthly_cohort_seed_user
    WHERE cohort_month = 8
    GROUP BY soldier_type
    HAVING MAX(total_mission_count) <> 10
);

INSERT INTO monthly_cohort_seed_assert (value)
SELECT NULL
WHERE EXISTS (
    SELECT user_account.user_id
    FROM monthly_cohort_seed_user seed
    INNER JOIN users user_account
        ON user_account.social_type = 'KAKAO'
       AND user_account.social_id = seed.social_id
    INNER JOIN codef_connection connection_info ON connection_info.user_id = user_account.user_id
    LEFT JOIN connected_account account ON account.connection_id = connection_info.connection_id
    GROUP BY user_account.user_id
    HAVING COUNT(account.account_id) <> 3
);

INSERT INTO monthly_cohort_seed_assert (value)
SELECT NULL
WHERE EXISTS (
    SELECT user_account.user_id
    FROM monthly_cohort_seed_user seed
    INNER JOIN users user_account
        ON user_account.social_type = 'KAKAO'
       AND user_account.social_id = seed.social_id
    INNER JOIN codef_connection connection_info ON connection_info.user_id = user_account.user_id
    INNER JOIN connected_account account ON account.connection_id = connection_info.connection_id
    LEFT JOIN transaction_history transaction_info ON transaction_info.account_id = account.account_id
    GROUP BY user_account.user_id
    HAVING COUNT(transaction_info.transaction_id) < 8
);

INSERT INTO monthly_cohort_seed_assert (value)
SELECT NULL
WHERE EXISTS (
    SELECT user_account.user_id
    FROM monthly_cohort_seed_user seed
    INNER JOIN users user_account
        ON user_account.social_type = 'KAKAO'
       AND user_account.social_id = seed.social_id
    LEFT JOIN simulation simulation_history
        ON simulation_history.user_id = user_account.user_id
       AND simulation_history.scenario_name = '월별 동기 기본 시뮬레이션'
    LEFT JOIN ai_analysis analysis_history
        ON analysis_history.user_id = user_account.user_id
       AND analysis_history.input_data_hash = SHA2(
           CONCAT(seed.social_id, '-analysis-2026-08-20'),
           256
       )
    GROUP BY user_account.user_id
    HAVING COUNT(DISTINCT simulation_history.simulation_id) <> 1
        OR COUNT(DISTINCT analysis_history.analysis_id) <> 1
);

COMMIT;

SELECT
    seed.soldier_type,
    seed.cohort_month AS enlistment_month,
    COUNT(DISTINCT user_account.user_id) AS user_count,
    MAX(summary.total_mission_count) AS first_place_mission_count,
    MIN(summary.total_mission_count) AS tenth_place_mission_count,
    COUNT(DISTINCT account.account_id) AS account_count,
    COUNT(DISTINCT transaction_info.transaction_id) AS transaction_count
FROM monthly_cohort_seed_user seed
INNER JOIN users user_account
    ON user_account.social_type = 'KAKAO'
   AND user_account.social_id = seed.social_id
INNER JOIN challenge_member member ON member.user_id = user_account.user_id
INNER JOIN challenge_member_summary summary ON summary.member_id = member.member_id
INNER JOIN codef_connection connection_info ON connection_info.user_id = user_account.user_id
INNER JOIN connected_account account ON account.connection_id = connection_info.connection_id
LEFT JOIN transaction_history transaction_info ON transaction_info.account_id = account.account_id
GROUP BY seed.soldier_type, seed.cohort_month
ORDER BY FIELD(seed.soldier_type, 'ARMY', 'NAVY', 'AIRFORCE', 'MARINE'), seed.cohort_month;

DROP TEMPORARY TABLE monthly_cohort_general_transaction;
DROP TEMPORARY TABLE monthly_cohort_account_plan;
DROP TEMPORARY TABLE monthly_cohort_seed_assert;
DROP TEMPORARY TABLE monthly_cohort_seed_user;
DROP TEMPORARY TABLE monthly_cohort_service;
