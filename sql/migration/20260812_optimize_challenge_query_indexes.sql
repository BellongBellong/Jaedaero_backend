-- 챌린지 그룹 조회와 미션 이벤트 조회에 사용하는 복합 인덱스를 추가합니다.

ALTER TABLE challenge_member
    ADD INDEX idx_challenge_member_user_joined_at (user_id, joined_at DESC);

ALTER TABLE leave_mode
    ADD INDEX idx_leave_mode_user_start_date (user_id, start_date);
