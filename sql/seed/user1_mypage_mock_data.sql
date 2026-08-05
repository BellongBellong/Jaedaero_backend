-- user1 마이페이지 프론트·백엔드 연결 테스트 데이터
-- 실행: mysql -u <DB_USER> -p jaedaero < sql/seed/user1_mypage_mock_data.sql
-- 수정된 badge / investment_badge 스키마를 기준으로 재실행 가능하게 작성했습니다.

START TRANSACTION;

SET @user1_social_id = 'user1';

-- 사용자 식별자는 social_type + social_id를 기준으로 재사용합니다.
INSERT INTO users (
    social_type,
    social_id,
    nickname,
    profile_image,
    profile_source,
    is_withdrawn,
    withdrawn_at
) VALUES (
    'KAKAO',
    @user1_social_id,
    '유노윤호최윤호',
    'ARMY',
    'GREEN',
    FALSE,
    NULL
)
ON DUPLICATE KEY UPDATE
    user_id = LAST_INSERT_ID(user_id),
    nickname = VALUES(nickname),
    profile_image = VALUES(profile_image),
    profile_source = VALUES(profile_source),
    is_withdrawn = FALSE,
    withdrawn_at = NULL;

SET @user1_id = LAST_INSERT_ID();

INSERT INTO soldier_profile (
    user_id,
    soldier_type,
    rank_name,
    enlistment_date,
    discharge_date,
    saving_join_yn
) VALUES (
    @user1_id,
    'ARMY',
    '병장',
    '2025-03-01',
    '2026-09-01',
    TRUE
)
ON DUPLICATE KEY UPDATE
    soldier_type = VALUES(soldier_type),
    rank_name = VALUES(rank_name),
    enlistment_date = VALUES(enlistment_date),
    discharge_date = VALUES(discharge_date),
    saving_join_yn = VALUES(saving_join_yn);

INSERT INTO goal (user_id, target_amount, target_date, status)
VALUES (@user1_id, 20000000, '2026-09-01', 'ACTIVE')
ON DUPLICATE KEY UPDATE
    target_amount = VALUES(target_amount),
    target_date = VALUES(target_date),
    status = VALUES(status);

-- 공격형 105회: 플래티넘(100회) 달성, 다이아(300회)까지 195회 남은 상태입니다.
-- 안정형 103회: 플래티넘(100회) 달성 상태입니다.
INSERT INTO investment_badge (
    user_id,
    initial_preference,
    badge_tier,
    safe_count,
    safe_grade,
    aggressive_count,
    aggressive_grade
) VALUES (
    @user1_id,
    'AGGRESSIVE',
    'AGGRESSIVE',
    103,
    'PLATINUM',
    105,
    'PLATINUM'
)
ON DUPLICATE KEY UPDATE
    initial_preference = VALUES(initial_preference),
    badge_tier = VALUES(badge_tier),
    safe_count = VALUES(safe_count),
    safe_grade = VALUES(safe_grade),
    aggressive_count = VALUES(aggressive_count),
    aggressive_grade = VALUES(aggressive_grade);

-- 누적 미션 수를 충족한 뱃지의 실제 획득 이력을 생성합니다.
DELETE FROM user_badge
WHERE user_id = @user1_id;

INSERT INTO user_badge (user_id, badge_id, acquired_at)
SELECT
    @user1_id,
    b.badge_id,
    TIMESTAMP('2026-08-05 09:41:00')
FROM badge b
JOIN investment_badge ib ON ib.user_id = @user1_id
WHERE b.is_active = TRUE
  AND b.required_completion_count <= CASE
      WHEN b.mission_type = 'SAFE' THEN ib.safe_count
      WHEN b.mission_type = 'AGGRESSIVE' THEN ib.aggressive_count
      ELSE 0
  END;

COMMIT;
