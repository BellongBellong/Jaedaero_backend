-- 챌린지 뱃지 등급 운영 기준 데이터입니다.
-- 실행: mysql -u <DB_USER> -p jaedaero < sql/seed/badge_policy.sql
-- 기존 사용자 획득 이력은 보존하고 현재 운영 등급만 활성화합니다.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET time_zone = '+09:00';
START TRANSACTION;

UPDATE badge
SET is_active = FALSE;

INSERT INTO badge (
    badge_name,
    badge_description,
    mission_type,
    required_completion_count,
    grade,
    is_active
)
VALUES
    ('안정형 브론즈', '안정형 미션 1개 완료', 'SAFE', 1, 'BRONZE', TRUE),
    ('안정형 실버', '안정형 미션 10개 완료', 'SAFE', 10, 'SILVER', TRUE),
    ('안정형 골드', '안정형 미션 50개 완료', 'SAFE', 50, 'GOLD', TRUE),
    ('안정형 플래티넘', '안정형 미션 100개 완료', 'SAFE', 100, 'PLATINUM', TRUE),
    ('안정형 다이아', '안정형 미션 300개 완료', 'SAFE', 300, 'DIAMOND', TRUE),
    ('공격형 브론즈', '공격형 미션 1개 완료', 'AGGRESSIVE', 1, 'BRONZE', TRUE),
    ('공격형 실버', '공격형 미션 10개 완료', 'AGGRESSIVE', 10, 'SILVER', TRUE),
    ('공격형 골드', '공격형 미션 50개 완료', 'AGGRESSIVE', 50, 'GOLD', TRUE),
    ('공격형 플래티넘', '공격형 미션 100개 완료', 'AGGRESSIVE', 100, 'PLATINUM', TRUE),
    ('공격형 다이아', '공격형 미션 300개 완료', 'AGGRESSIVE', 300, 'DIAMOND', TRUE)
ON DUPLICATE KEY UPDATE
    badge_name = VALUES(badge_name),
    badge_description = VALUES(badge_description),
    required_completion_count = VALUES(required_completion_count),
    is_active = TRUE;

COMMIT;
