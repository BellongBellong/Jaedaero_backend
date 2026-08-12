-- 챌린지 입대월 동기 랭킹 Mock 데이터
-- 선행 실행: sql/seed/challenge_mission_mock_data.sql
-- 실행: mysql -u <DB_USER> -p jaedaero < sql/seed/cohort_test_users.sql
-- 테스트 기준 사용자: social_id = challenge-mock-06 (누적 45개, 동기 6위)

START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS challenge_mock_user;
CREATE TEMPORARY TABLE challenge_mock_user (
    social_id VARCHAR(255) NOT NULL PRIMARY KEY,
    nickname VARCHAR(50) NOT NULL,
    rank_name VARCHAR(20) NOT NULL,
    profile_source ENUM('GREEN', 'OLIVE', 'YELLOW', 'ORANGE', 'GRAY', 'BLACK') NOT NULL,
    total_mission_count INT NOT NULL,
    monthly_mission_count INT NOT NULL,
    safe_count INT NOT NULL,
    aggressive_count INT NOT NULL
) DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

INSERT INTO challenge_mock_user (
    social_id,
    nickname,
    rank_name,
    profile_source,
    total_mission_count,
    monthly_mission_count,
    safe_count,
    aggressive_count
) VALUES
    ('challenge-mock-01', '재대로', '병장', 'GREEN', 60, 18, 30, 30),
    ('challenge-mock-02', '박병장', '병장', 'OLIVE', 57, 17, 29, 28),
    ('challenge-mock-03', '성훈짱', '상병', 'YELLOW', 55, 16, 27, 28),
    ('challenge-mock-04', '김상병', '상병', 'ORANGE', 52, 15, 26, 26),
    ('challenge-mock-05', '이일병', '일병', 'GRAY', 50, 14, 25, 25),
    ('challenge-mock-06', '테스트나', '일병', 'GREEN', 45, 13, 22, 23),
    ('challenge-mock-07', '최일병', '일병', 'OLIVE', 42, 12, 21, 21),
    ('challenge-mock-08', '정이병', '이병', 'YELLOW', 37, 10, 18, 19),
    ('challenge-mock-09', '한이병', '이병', 'ORANGE', 33, 9, 16, 17),
    ('challenge-mock-10', '윤이병', '이병', 'GRAY', 30, 8, 15, 15),
    ('challenge-mock-11', '강훈련병', '이병', 'GREEN', 25, 6, 12, 13),
    ('challenge-mock-12', '조훈련병', '이병', 'OLIVE', 18, 4, 9, 9);

INSERT INTO users (
    social_type,
    social_id,
    nickname,
    profile_image,
    profile_source,
    is_withdrawn,
    withdrawn_at
)
SELECT
    'KAKAO',
    cmu.social_id,
    cmu.nickname,
    'ARMY',
    cmu.profile_source,
    FALSE,
    NULL
FROM challenge_mock_user cmu
ON DUPLICATE KEY UPDATE
    nickname = VALUES(nickname),
    profile_image = VALUES(profile_image),
    profile_source = VALUES(profile_source),
    is_withdrawn = FALSE,
    withdrawn_at = NULL;

INSERT INTO soldier_profile (
    user_id,
    soldier_type,
    rank_name,
    enlistment_date,
    discharge_date,
    saving_join_yn
)
SELECT
    u.user_id,
    'ARMY',
    cmu.rank_name,
    DATE_SUB(CURDATE(), INTERVAL DAYOFMONTH(CURDATE()) - 1 DAY),
    DATE_ADD(CURDATE(), INTERVAL 100 DAY),
    TRUE
FROM users u
INNER JOIN challenge_mock_user cmu ON cmu.social_id = u.social_id
WHERE u.social_type = 'KAKAO'
ON DUPLICATE KEY UPDATE
    soldier_type = VALUES(soldier_type),
    rank_name = VALUES(rank_name),
    enlistment_date = VALUES(enlistment_date),
    discharge_date = VALUES(discharge_date),
    saving_join_yn = VALUES(saving_join_yn);

INSERT INTO challenge_group (
    soldier_type,
    enlistment_year,
    enlistment_month
) VALUES (
    'ARMY',
    YEAR(CURDATE()),
    MONTH(CURDATE())
)
ON DUPLICATE KEY UPDATE
    group_id = LAST_INSERT_ID(group_id);

INSERT INTO challenge_member (
    group_id,
    user_id
)
SELECT
    cg.group_id,
    u.user_id
FROM challenge_group cg
INNER JOIN users u ON u.social_type = 'KAKAO'
INNER JOIN challenge_mock_user cmu ON cmu.social_id = u.social_id
WHERE cg.soldier_type = 'ARMY'
  AND cg.enlistment_year = YEAR(CURDATE())
  AND cg.enlistment_month = MONTH(CURDATE())
ON DUPLICATE KEY UPDATE
    joined_at = CURRENT_TIMESTAMP;

INSERT INTO challenge_member_summary (
    member_id,
    total_mission_count
)
SELECT
    cm.member_id,
    cmu.total_mission_count
FROM challenge_member cm
INNER JOIN users u ON u.user_id = cm.user_id
INNER JOIN challenge_mock_user cmu ON cmu.social_id = u.social_id
ON DUPLICATE KEY UPDATE
    total_mission_count = VALUES(total_mission_count),
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO challenge_monthly_result (
    member_id,
    result_month,
    mission_completion_count
)
SELECT
    cm.member_id,
    DATE_SUB(CURDATE(), INTERVAL DAYOFMONTH(CURDATE()) - 1 DAY),
    cmu.monthly_mission_count
FROM challenge_member cm
INNER JOIN users u ON u.user_id = cm.user_id
INNER JOIN challenge_mock_user cmu ON cmu.social_id = u.social_id
ON DUPLICATE KEY UPDATE
    mission_completion_count = VALUES(mission_completion_count);

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
    u.user_id,
    'BALANCED',
    'BALANCED',
    cmu.safe_count,
    CASE
        WHEN cmu.safe_count >= 300 THEN 'DIAMOND'
        WHEN cmu.safe_count >= 100 THEN 'PLATINUM'
        WHEN cmu.safe_count >= 50 THEN 'GOLD'
        WHEN cmu.safe_count >= 10 THEN 'SILVER'
        WHEN cmu.safe_count >= 1 THEN 'BRONZE'
        ELSE NULL
    END,
    cmu.aggressive_count,
    CASE
        WHEN cmu.aggressive_count >= 300 THEN 'DIAMOND'
        WHEN cmu.aggressive_count >= 100 THEN 'PLATINUM'
        WHEN cmu.aggressive_count >= 50 THEN 'GOLD'
        WHEN cmu.aggressive_count >= 10 THEN 'SILVER'
        WHEN cmu.aggressive_count >= 1 THEN 'BRONZE'
        ELSE NULL
    END
FROM users u
INNER JOIN challenge_mock_user cmu ON cmu.social_id = u.social_id
ON DUPLICATE KEY UPDATE
    initial_preference = VALUES(initial_preference),
    badge_tier = VALUES(badge_tier),
    safe_count = VALUES(safe_count),
    safe_grade = VALUES(safe_grade),
    aggressive_count = VALUES(aggressive_count),
    aggressive_grade = VALUES(aggressive_grade);

-- 테스트 기준 사용자의 오늘 미션 완료 상태를 구성합니다.
INSERT INTO user_mission_completion (
    user_id,
    mission_id,
    completion_date
)
SELECT
    u.user_id,
    m.mission_id,
    CURDATE()
FROM users u
INNER JOIN mission m ON (
    (m.mission_category = 'DAILY' AND m.display_order = 1)
    OR (m.mission_category = 'RECOMMENDED' AND m.mission_type = 'SAFE' AND m.display_order = 1)
    OR (m.mission_category = 'RECOMMENDED' AND m.mission_type = 'AGGRESSIVE' AND m.display_order = 1)
)
WHERE u.social_type = 'KAKAO'
  AND u.social_id = 'challenge-mock-06'
ON DUPLICATE KEY UPDATE
    completed_at = CURRENT_TIMESTAMP;

DROP TEMPORARY TABLE challenge_mock_user;

COMMIT;
