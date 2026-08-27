-- 기존 알림 이력은 보존하면서, 알림 파이프라인 도입 이후 누락된 스키마를 보정합니다.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

ALTER TABLE notification_history
    ADD COLUMN deep_link VARCHAR(500) NULL AFTER body,
    ADD COLUMN dedupe_key VARCHAR(200) NULL AFTER deep_link,
    ADD COLUMN push_status ENUM('PENDING', 'PROCESSING', 'SENT', 'FAILED', 'SKIPPED') NOT NULL DEFAULT 'PENDING' AFTER is_read,
    ADD COLUMN push_attempts INT NOT NULL DEFAULT 0 AFTER push_status,
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER read_at,
    ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at;

UPDATE notification_history
SET dedupe_key = CONCAT('legacy-notification-', notification_id)
WHERE dedupe_key IS NULL OR dedupe_key = '';

ALTER TABLE notification_history
    MODIFY COLUMN dedupe_key VARCHAR(200) NOT NULL,
    ADD UNIQUE KEY uq_notification_history_dedupe (dedupe_key),
    ADD KEY idx_notification_history_user_created (user_id, created_at DESC, notification_id DESC),
    ADD KEY idx_notification_history_user_unread (user_id, is_read);

CREATE TABLE IF NOT EXISTS notification_outbox (
    outbox_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id CHAR(36) NOT NULL,
    notification_id BIGINT NULL,
    campaign_id BIGINT NULL,
    dedupe_key VARCHAR(200) NOT NULL,
    status ENUM('PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED') NOT NULL DEFAULT 'PENDING',
    publish_attempts INT NOT NULL DEFAULT 0,
    available_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    claimed_at TIMESTAMP NULL,
    published_at TIMESTAMP NULL,
    last_error VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_notification_outbox_event (event_id),
    UNIQUE KEY uq_notification_outbox_dedupe (dedupe_key),
    KEY idx_notification_outbox_publish (status, available_at, outbox_id),
    CONSTRAINT chk_notification_outbox_target CHECK ((notification_id IS NOT NULL) <> (campaign_id IS NOT NULL)),
    CONSTRAINT fk_notification_outbox_notification FOREIGN KEY (notification_id) REFERENCES notification_history(notification_id) ON DELETE CASCADE
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
