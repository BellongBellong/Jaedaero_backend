-- 챌린지 입대월 동기 랭킹 Mock 데이터
-- 선행 실행: sql/seed/challenge_mission_mock_data.sql
-- 실행: mysql -u <DB_USER> -p jaedaero < sql/seed/cohort_test_users.sql
-- 100명 규모의 동기 랭킹 Mock 데이터입니다.
-- 테스트 기준 사용자: social_id = challenge-mock-06 (누적 45개)

-- Railway MySQL을 포함해 실행 세션의 한글 인코딩을 명시합니다.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
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

-- 상위권부터 하위권까지 점수가 완만하게 분포된 88명의 동기 데이터를 추가합니다.
-- 재실행해도 social_id를 기준으로 같은 사용자 데이터가 갱신됩니다.
INSERT INTO challenge_mock_user (
    social_id,
    nickname,
    rank_name,
    profile_source,
    total_mission_count,
    monthly_mission_count,
    safe_count,
    aggressive_count
)
WITH RECURSIVE sequence AS (
    SELECT 13 AS user_no
    UNION ALL
    SELECT user_no + 1
    FROM sequence
    WHERE user_no < 100
)
SELECT
    CONCAT('challenge-mock-', user_no),
    CONCAT('동기', LPAD(user_no, 3, '0')),
    CASE MOD(user_no, 4)
        WHEN 0 THEN '병장'
        WHEN 1 THEN '상병'
        WHEN 2 THEN '일병'
        ELSE '이병'
    END,
    CASE MOD(user_no, 6)
        WHEN 0 THEN 'GREEN'
        WHEN 1 THEN 'OLIVE'
        WHEN 2 THEN 'YELLOW'
        WHEN 3 THEN 'ORANGE'
        WHEN 4 THEN 'GRAY'
        ELSE 'BLACK'
    END,
    GREATEST(8, 59 - FLOOR((user_no - 13) * 0.55)),
    GREATEST(1, FLOOR(GREATEST(8, 59 - FLOOR((user_no - 13) * 0.55)) * 0.3)),
    CASE MOD(user_no, 5)
        WHEN 0 THEN 320 + MOD(user_no, 20)
        WHEN 1 THEN 120 + MOD(user_no, 30)
        WHEN 2 THEN 70 + MOD(user_no, 25)
        WHEN 3 THEN 25 + MOD(user_no, 20)
        ELSE 3 + MOD(user_no, 7)
    END,
    CASE MOD(user_no + 2, 5)
        WHEN 0 THEN 320 + MOD(user_no, 20)
        WHEN 1 THEN 120 + MOD(user_no, 30)
        WHEN 2 THEN 70 + MOD(user_no, 25)
        WHEN 3 THEN 25 + MOD(user_no, 20)
        ELSE 3 + MOD(user_no, 7)
    END
FROM sequence;

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

-- 동기 랭킹 목업 사용자의 전역 자산 목표를 함께 생성합니다.
INSERT INTO goal (
    user_id,
    target_amount,
    target_date,
    status
)
SELECT
    u.user_id,
    CASE MOD(CAST(SUBSTRING_INDEX(cmu.social_id, '-', -1) AS UNSIGNED), 5)
        WHEN 0 THEN 30000000
        WHEN 1 THEN 10000000
        WHEN 2 THEN 15000000
        WHEN 3 THEN 20000000
        ELSE 25000000
    END,
    DATE_ADD(
        CURDATE(),
        INTERVAL 120 + MOD(CAST(SUBSTRING_INDEX(cmu.social_id, '-', -1) AS UNSIGNED), 24) * 15 DAY),
    'ACTIVE'
FROM users u
INNER JOIN challenge_mock_user cmu ON cmu.social_id = u.social_id
WHERE u.social_type = 'KAKAO'
ON DUPLICATE KEY UPDATE
    target_amount = VALUES(target_amount),
    target_date = VALUES(target_date),
    status = VALUES(status);

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
INNER JOIN challenge_group cg ON cg.group_id = cm.group_id
INNER JOIN users u ON u.user_id = cm.user_id
INNER JOIN challenge_mock_user cmu ON cmu.social_id = u.social_id
WHERE cg.soldier_type = 'ARMY'
  AND cg.enlistment_year = YEAR(CURDATE())
  AND cg.enlistment_month = MONTH(CURDATE())
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
INNER JOIN challenge_group cg ON cg.group_id = cm.group_id
INNER JOIN users u ON u.user_id = cm.user_id
INNER JOIN challenge_mock_user cmu ON cmu.social_id = u.social_id
WHERE cg.soldier_type = 'ARMY'
  AND cg.enlistment_year = YEAR(CURDATE())
  AND cg.enlistment_month = MONTH(CURDATE())
ON DUPLICATE KEY UPDATE
    mission_completion_count = VALUES(mission_completion_count);

-- 최근 완료 이력: 미션 화면과 완료 상태 테스트를 위해 모든 동기에게 누적 미션 수만큼 기록합니다.
-- 뱃지 수치는 서비스 이전의 누적 활동까지 포함한 값으로 아래에서 별도로 설정합니다.
INSERT INTO user_mission_completion (
    user_id,
    mission_id,
    completion_date
)
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
), mission_count AS (
    SELECT COUNT(*) AS count
    FROM ordered_mission
)
SELECT
    u.user_id,
    om.mission_id,
    DATE_SUB(CURDATE(), INTERVAL FLOOR((cs.completion_no - 1) / mc.count) DAY)
FROM challenge_mock_user cmu
INNER JOIN users u
    ON u.social_type = 'KAKAO'
   AND u.social_id = cmu.social_id
INNER JOIN completion_sequence cs
    ON cs.completion_no <= cmu.total_mission_count
CROSS JOIN mission_count mc
INNER JOIN ordered_mission om
    ON om.mission_no = MOD(cs.completion_no - 1, mc.count) + 1
WHERE TRUE
ON DUPLICATE KEY UPDATE
    completed_at = CURRENT_TIMESTAMP;

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
    CASE MOD(CAST(SUBSTRING_INDEX(cmu.social_id, '-', -1) AS UNSIGNED), 3)
        WHEN 0 THEN 'SAFE'
        WHEN 1 THEN 'BALANCED'
        ELSE 'AGGRESSIVE'
    END,
    CASE MOD(CAST(SUBSTRING_INDEX(cmu.social_id, '-', -1) AS UNSIGNED), 3)
        WHEN 0 THEN 'SAFE'
        WHEN 1 THEN 'BALANCED'
        ELSE 'AGGRESSIVE'
    END,
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

-- 집계 수치에 해당하는 모든 뱃지 획득 이력도 함께 생성합니다.
INSERT INTO user_badge (
    user_id,
    badge_id,
    acquired_at
)
SELECT
    ib.user_id,
    b.badge_id,
    DATE_SUB(CURRENT_TIMESTAMP, INTERVAL MOD(ib.safe_count + ib.aggressive_count, 30) DAY)
FROM investment_badge ib
INNER JOIN users u ON u.user_id = ib.user_id
INNER JOIN challenge_mock_user cmu ON cmu.social_id = u.social_id
INNER JOIN badge b ON (
    (b.mission_type = 'SAFE' AND b.required_completion_count <= ib.safe_count)
    OR (b.mission_type = 'AGGRESSIVE' AND b.required_completion_count <= ib.aggressive_count)
)
ON DUPLICATE KEY UPDATE
    acquired_at = VALUES(acquired_at);

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
