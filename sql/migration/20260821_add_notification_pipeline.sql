-- FCM + Redis Streams 알림 기능 도입 전용 마이그레이션.
-- 기존 notification 테이블은 Java 구현 없이 배포된 초안이므로 새 계약으로 교체한다.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET time_zone = '+09:00';
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS notification_outbox;
DROP TABLE IF EXISTS notification_campaign_receipt;
DROP TABLE IF EXISTS notification_campaign;
DROP TABLE IF EXISTS notification_history;
DROP TABLE IF EXISTS device_token;

CREATE TABLE device_token (
    device_token_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    fcm_token VARCHAR(500) NOT NULL,
    device_type ENUM('IOS', 'ANDROID', 'WEB') NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_device_token_fcm (fcm_token),
    KEY idx_device_token_user_active (user_id, is_active),
    CONSTRAINT fk_device_token_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notification_history (
    notification_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    body VARCHAR(500) NULL,
    deep_link VARCHAR(500) NULL,
    dedupe_key VARCHAR(200) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    push_status ENUM('PENDING', 'PROCESSING', 'SENT', 'FAILED', 'SKIPPED') NOT NULL DEFAULT 'PENDING',
    push_attempts INT NOT NULL DEFAULT 0,
    sent_at TIMESTAMP NULL,
    read_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_notification_history_dedupe (dedupe_key),
    KEY idx_notification_history_user_created (user_id, created_at DESC, notification_id DESC),
    KEY idx_notification_history_user_unread (user_id, is_read),
    CONSTRAINT fk_notification_history_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notification_campaign (
    campaign_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    notification_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    body VARCHAR(500) NULL,
    deep_link VARCHAR(500) NULL,
    topic VARCHAR(200) NOT NULL,
    dedupe_key VARCHAR(200) NOT NULL,
    push_status ENUM('PENDING', 'PROCESSING', 'SENT', 'FAILED', 'SKIPPED') NOT NULL DEFAULT 'PENDING',
    push_attempts INT NOT NULL DEFAULT 0,
    visible_from TIMESTAMP NOT NULL,
    visible_until TIMESTAMP NULL,
    sent_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_notification_campaign_dedupe (dedupe_key),
    KEY idx_notification_campaign_visible (visible_from, visible_until, created_at)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notification_campaign_receipt (
    campaign_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    read_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (campaign_id, user_id),
    CONSTRAINT fk_notification_campaign_receipt_campaign FOREIGN KEY (campaign_id) REFERENCES notification_campaign(campaign_id) ON DELETE CASCADE,
    CONSTRAINT fk_notification_campaign_receipt_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notification_outbox (
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
    CONSTRAINT fk_notification_outbox_notification FOREIGN KEY (notification_id) REFERENCES notification_history(notification_id) ON DELETE CASCADE,
    CONSTRAINT fk_notification_outbox_campaign FOREIGN KEY (campaign_id) REFERENCES notification_campaign(campaign_id) ON DELETE CASCADE
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
