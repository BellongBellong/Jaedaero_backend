-- 시연용 동기 챌린지 데이터: 육군 2026년 5월 입대 동기 50명
-- 실행 전 기존 demo-cohort-* 사용자만 제거하므로 반복 실행해도 50명으로 유지됩니다.

DELETE FROM users
WHERE social_id LIKE 'demo-cohort-%';

INSERT INTO users (social_type, social_id, nickname, profile_image, profile_source)
VALUES
  ('KAKAO', 'demo-cohort-01', '김도윤', 'ARMY', 'GREEN'),
  ('KAKAO', 'demo-cohort-02', '김민준', 'ARMY', 'OLIVE'),
  ('KAKAO', 'demo-cohort-03', '김서준', 'ARMY', 'YELLOW'),
  ('KAKAO', 'demo-cohort-04', '김예준', 'ARMY', 'ORANGE'),
  ('KAKAO', 'demo-cohort-05', '김지훈', 'ARMY', 'GRAY'),
  ('KAKAO', 'demo-cohort-06', '김현우', 'ARMY', 'BLACK'),
  ('KAKAO', 'demo-cohort-07', '김준서', 'ARMY', 'GREEN'),
  ('KAKAO', 'demo-cohort-08', '김도현', 'ARMY', 'OLIVE'),
  ('KAKAO', 'demo-cohort-09', '김우진', 'ARMY', 'YELLOW'),
  ('KAKAO', 'demo-cohort-10', '김건우', 'ARMY', 'ORANGE'),
  ('KAKAO', 'demo-cohort-11', '이도윤', 'ARMY', 'GRAY'),
  ('KAKAO', 'demo-cohort-12', '이민준', 'ARMY', 'BLACK'),
  ('KAKAO', 'demo-cohort-13', '이서준', 'ARMY', 'GREEN'),
  ('KAKAO', 'demo-cohort-14', '이예준', 'ARMY', 'OLIVE'),
  ('KAKAO', 'demo-cohort-15', '이지훈', 'ARMY', 'YELLOW'),
  ('KAKAO', 'demo-cohort-16', '이현우', 'ARMY', 'ORANGE'),
  ('KAKAO', 'demo-cohort-17', '이준서', 'ARMY', 'GRAY'),
  ('KAKAO', 'demo-cohort-18', '이도현', 'ARMY', 'BLACK'),
  ('KAKAO', 'demo-cohort-19', '이우진', 'ARMY', 'GREEN'),
  ('KAKAO', 'demo-cohort-20', '이건우', 'ARMY', 'OLIVE'),
  ('KAKAO', 'demo-cohort-21', '박도윤', 'ARMY', 'YELLOW'),
  ('KAKAO', 'demo-cohort-22', '박민준', 'ARMY', 'ORANGE'),
  ('KAKAO', 'demo-cohort-23', '박서준', 'ARMY', 'GRAY'),
  ('KAKAO', 'demo-cohort-24', '박예준', 'ARMY', 'BLACK'),
  ('KAKAO', 'demo-cohort-25', '박지훈', 'ARMY', 'GREEN'),
  ('KAKAO', 'demo-cohort-26', '박현우', 'ARMY', 'OLIVE'),
  ('KAKAO', 'demo-cohort-27', '박준서', 'ARMY', 'YELLOW'),
  ('KAKAO', 'demo-cohort-28', '박도현', 'ARMY', 'ORANGE'),
  ('KAKAO', 'demo-cohort-29', '박우진', 'ARMY', 'GRAY'),
  ('KAKAO', 'demo-cohort-30', '박건우', 'ARMY', 'BLACK'),
  ('KAKAO', 'demo-cohort-31', '최도윤', 'ARMY', 'GREEN'),
  ('KAKAO', 'demo-cohort-32', '최민준', 'ARMY', 'OLIVE'),
  ('KAKAO', 'demo-cohort-33', '최서준', 'ARMY', 'YELLOW'),
  ('KAKAO', 'demo-cohort-34', '최예준', 'ARMY', 'ORANGE'),
  ('KAKAO', 'demo-cohort-35', '최지훈', 'ARMY', 'GRAY'),
  ('KAKAO', 'demo-cohort-36', '최현우', 'ARMY', 'BLACK'),
  ('KAKAO', 'demo-cohort-37', '최준서', 'ARMY', 'GREEN'),
  ('KAKAO', 'demo-cohort-38', '최도현', 'ARMY', 'OLIVE'),
  ('KAKAO', 'demo-cohort-39', '최우진', 'ARMY', 'YELLOW'),
  ('KAKAO', 'demo-cohort-40', '최건우', 'ARMY', 'ORANGE'),
  ('KAKAO', 'demo-cohort-41', '정도윤', 'ARMY', 'GRAY'),
  ('KAKAO', 'demo-cohort-42', '정민준', 'ARMY', 'BLACK'),
  ('KAKAO', 'demo-cohort-43', '정서준', 'ARMY', 'GREEN'),
  ('KAKAO', 'demo-cohort-44', '정예준', 'ARMY', 'OLIVE'),
  ('KAKAO', 'demo-cohort-45', '정지훈', 'ARMY', 'YELLOW'),
  ('KAKAO', 'demo-cohort-46', '정현우', 'ARMY', 'ORANGE'),
  ('KAKAO', 'demo-cohort-47', '정준서', 'ARMY', 'GRAY'),
  ('KAKAO', 'demo-cohort-48', '정도현', 'ARMY', 'BLACK'),
  ('KAKAO', 'demo-cohort-49', '정우진', 'ARMY', 'GREEN'),
  ('KAKAO', 'demo-cohort-50', '정건우', 'ARMY', 'OLIVE');

INSERT INTO soldier_profile (user_id, soldier_type, rank_name, enlistment_date, discharge_date, saving_join_yn)
SELECT user_id, 'ARMY', '일병', '2026-05-01', '2027-11-01', TRUE
FROM users
WHERE social_id LIKE 'demo-cohort-%';

INSERT INTO challenge_group (soldier_type, enlistment_year, enlistment_month)
VALUES ('ARMY', 2026, 5)
ON DUPLICATE KEY UPDATE group_id = LAST_INSERT_ID(group_id);

INSERT INTO challenge_member (group_id, user_id)
SELECT cg.group_id, u.user_id
FROM users u
JOIN challenge_group cg
  ON cg.soldier_type = 'ARMY'
 AND cg.enlistment_year = 2026
 AND cg.enlistment_month = 5
WHERE u.social_id LIKE 'demo-cohort-%';

-- 동기 화면에 자연스러운 랭킹을 보여 주기 위한 월간·누적 미션 완료 수입니다.
INSERT INTO challenge_monthly_result (member_id, result_month, mission_completion_count)
SELECT cm.member_id, DATE_FORMAT(CURDATE(), '%Y-%m-01'), 3 + MOD(cm.user_id, 8)
FROM challenge_member cm
JOIN users u ON u.user_id = cm.user_id
WHERE u.social_id LIKE 'demo-cohort-%';

INSERT INTO challenge_member_summary (member_id, total_mission_count)
SELECT cm.member_id, 12 + MOD(cm.user_id, 25)
FROM challenge_member cm
JOIN users u ON u.user_id = cm.user_id
WHERE u.social_id LIKE 'demo-cohort-%';
