-- 예금상품 미션을 적립식 투자 가이드 미션으로 교체하고 투자 추천 이동 키를 바로잡니다.
-- 기존 완료 이력은 보존하고, 더 이상 제공하지 않는 예금상품 미션만 비활성화합니다.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET time_zone = '+09:00';
START TRANSACTION;

UPDATE mission
SET action_type = 'VIEW_DEPOSIT_PRODUCT',
    is_active = TRUE
WHERE mission_code = 'AGGRESSIVE_REBALANCING';

INSERT INTO mission (
    mission_code,
    mission_type,
    mission_category,
    title,
    description,
    action_type,
    display_order,
    trigger_type,
    trigger_value,
    event_priority,
    is_active
)
VALUES (
    'SAFE_INVESTMENT_GUIDANCE',
    'SAFE',
    'RECOMMENDED',
    '적립식 투자 가이드 확인하기',
    '나에게 맞는 적립식 투자 가이드를 확인해보세요.',
    'VIEW_REBALANCING',
    1,
    'NONE',
    NULL,
    0,
    TRUE
)
ON DUPLICATE KEY UPDATE
    mission_type = VALUES(mission_type),
    mission_category = VALUES(mission_category),
    title = VALUES(title),
    description = VALUES(description),
    action_type = VALUES(action_type),
    display_order = VALUES(display_order),
    trigger_type = VALUES(trigger_type),
    trigger_value = VALUES(trigger_value),
    event_priority = VALUES(event_priority),
    is_active = VALUES(is_active);

UPDATE mission
SET is_active = FALSE
WHERE mission_code = 'SAFE_DEPOSIT_PRODUCT';

COMMIT;
