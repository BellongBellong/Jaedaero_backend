-- 동기 챌린지 및 온보딩 API 테스트용 사용자 5명
-- 실행: mysql -u <DB_USER> -p jaedaero < sql/seed/cohort_test_users.sql
-- 모든 사용자는 육군(ARMY), 2026년 4월 입대로 동일한 동기 그룹에 속합니다.

START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS cohort_test_user;
CREATE TEMPORARY TABLE cohort_test_user (
    social_id VARCHAR(255) NOT NULL PRIMARY KEY,
    nickname VARCHAR(50) NOT NULL,
    rank_name VARCHAR(20) NOT NULL,
    profile_source ENUM('GREEN', 'OLIVE', 'YELLOW', 'ORANGE', 'GRAY', 'BLACK') NOT NULL,
    initial_preference ENUM('SAFE', 'BALANCED', 'AGGRESSIVE') NOT NULL,
    target_amount BIGINT NOT NULL,
    saving_rate DECIMAL(5, 2) NOT NULL,
    ranking_no INT NOT NULL
) DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

INSERT INTO cohort_test_user (
    social_id, nickname, rank_name, profile_source, initial_preference,
    target_amount, saving_rate, ranking_no
) VALUES
    ('test-cohort-202604-01', '동기테스트01', '일병', 'GREEN', 'SAFE', 15000000, 82.50, 2),
    ('test-cohort-202604-02', '동기테스트02', '일병', 'OLIVE', 'BALANCED', 18000000, 88.10, 1),
    ('test-cohort-202604-03', '동기테스트03', '상병', 'YELLOW', 'AGGRESSIVE', 12000000, 74.30, 4),
    ('test-cohort-202604-04', '동기테스트04', '상병', 'ORANGE', 'BALANCED', 14000000, 79.80, 3),
    ('test-cohort-202604-05', '동기테스트05', '병장', 'GRAY', 'SAFE', 10000000, 68.40, 5);

INSERT INTO users (
    social_type, social_id, nickname, profile_image, profile_source, is_withdrawn
)
SELECT
    'KAKAO', c.social_id, c.nickname, 'ARMY', c.profile_source, FALSE
FROM cohort_test_user c
ON DUPLICATE KEY UPDATE
    nickname = VALUES(nickname),
    profile_image = VALUES(profile_image),
    profile_source = VALUES(profile_source),
    is_withdrawn = FALSE,
    withdrawn_at = NULL;

INSERT INTO soldier_profile (
    user_id, soldier_type, rank_name, enlistment_date, discharge_date, saving_join_yn
)
SELECT
    u.user_id, 'ARMY', c.rank_name, '2026-04-01', '2027-10-01', FALSE
FROM users u
JOIN cohort_test_user c ON c.social_id = u.social_id
WHERE u.social_type = 'KAKAO'
ON DUPLICATE KEY UPDATE
    soldier_type = VALUES(soldier_type),
    rank_name = VALUES(rank_name),
    enlistment_date = VALUES(enlistment_date),
    discharge_date = VALUES(discharge_date),
    saving_join_yn = VALUES(saving_join_yn);

INSERT INTO user_agreement (
    user_id, agreement_type, agreement_version, is_required
)
SELECT
    u.user_id, agreement.agreement_type, 'v1.0', agreement.is_required
FROM users u
JOIN cohort_test_user c ON c.social_id = u.social_id
CROSS JOIN (
    SELECT 'SERVICE_USE' AS agreement_type, TRUE AS is_required
    UNION ALL SELECT 'PERSONAL_INFORMATION_COLLECTION', TRUE
    UNION ALL SELECT 'FINANCIAL_INFORMATION_INQUIRY', TRUE
    UNION ALL SELECT 'AI_SERVICE_USE', TRUE
    UNION ALL SELECT 'MARKETING_INFORMATION_RECEIPT', FALSE
) agreement
WHERE u.social_type = 'KAKAO'
ON DUPLICATE KEY UPDATE
    is_required = VALUES(is_required),
    agreed_at = CURRENT_TIMESTAMP;

INSERT INTO goal (user_id, target_amount, target_date, status)
SELECT
    u.user_id, c.target_amount, '2027-10-01', 'ACTIVE'
FROM users u
JOIN cohort_test_user c ON c.social_id = u.social_id
WHERE u.social_type = 'KAKAO'
ON DUPLICATE KEY UPDATE
    target_amount = VALUES(target_amount),
    target_date = VALUES(target_date),
    status = VALUES(status);

INSERT INTO investment_badge (user_id, initial_preference, badge_tier)
SELECT
    u.user_id, c.initial_preference, c.initial_preference
FROM users u
JOIN cohort_test_user c ON c.social_id = u.social_id
WHERE u.social_type = 'KAKAO'
ON DUPLICATE KEY UPDATE
    initial_preference = VALUES(initial_preference),
    badge_tier = VALUES(badge_tier);

INSERT INTO challenge_group (soldier_type, enlistment_year, enlistment_month)
VALUES ('ARMY', 2026, 4)
ON DUPLICATE KEY UPDATE
    group_id = LAST_INSERT_ID(group_id);

INSERT INTO challenge_member (group_id, user_id)
SELECT
    cg.group_id, u.user_id
FROM challenge_group cg
JOIN users u ON u.social_type = 'KAKAO'
JOIN cohort_test_user c ON c.social_id = u.social_id
WHERE cg.soldier_type = 'ARMY'
  AND cg.enlistment_year = 2026
  AND cg.enlistment_month = 4
ON DUPLICATE KEY UPDATE
    joined_at = CURRENT_TIMESTAMP;

INSERT INTO challenge_monthly_result (
    member_id, result_month, saving_rate, ranking_no
)
SELECT
    cm.member_id, '2026-08-01', c.saving_rate, c.ranking_no
FROM challenge_member cm
JOIN challenge_group cg ON cg.group_id = cm.group_id
JOIN users u ON u.user_id = cm.user_id
JOIN cohort_test_user c ON c.social_id = u.social_id
WHERE cg.soldier_type = 'ARMY'
  AND cg.enlistment_year = 2026
  AND cg.enlistment_month = 4
ON DUPLICATE KEY UPDATE
    saving_rate = VALUES(saving_rate),
    ranking_no = VALUES(ranking_no);

DROP TEMPORARY TABLE cohort_test_user;

COMMIT;
