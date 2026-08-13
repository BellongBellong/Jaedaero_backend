-- 투자 뱃지 집계 수를 기준으로 누락된 사용자 뱃지 획득 이력을 보정합니다.
-- 이미 존재하는 이력은 중복 생성하지 않습니다.

INSERT INTO user_badge (
    user_id,
    badge_id
)
SELECT
    ib.user_id,
    b.badge_id
FROM investment_badge ib
INNER JOIN badge b
    ON (
        (b.mission_type = 'SAFE' AND b.required_completion_count <= ib.safe_count)
        OR (b.mission_type = 'AGGRESSIVE' AND b.required_completion_count <= ib.aggressive_count)
    )
LEFT JOIN user_badge ub
    ON ub.user_id = ib.user_id
   AND ub.badge_id = b.badge_id
WHERE b.is_active = TRUE
  AND ub.user_badge_id IS NULL;
