-- 거래내역 자동 동기화 API가 조회한 CODEF 기간을 기록한다.
-- 실행: mysql -u <DB_USER> -p jaedaero_db < sql/migration/20260812_add_account_transaction_sync.sql

CREATE TABLE IF NOT EXISTS account_transaction_sync (
    transaction_sync_id  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '거래내역 동기화 ID',
    account_id           BIGINT NOT NULL COMMENT '연동 계좌 ID',
    inquiry_type         ENUM('DEMAND_DEPOSIT', 'INSTALLMENT_SAVINGS') NOT NULL COMMENT '거래 조회 유형',
    requested_start_date DATE NOT NULL COMMENT 'CODEF 조회 시작일',
    requested_end_date   DATE NOT NULL COMMENT 'CODEF 조회 종료일',
    synced_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '동기화 일시',

    CONSTRAINT uq_account_transaction_sync_period
        UNIQUE (account_id, inquiry_type, requested_start_date, requested_end_date),
    CONSTRAINT fk_account_transaction_sync_account
        FOREIGN KEY (account_id) REFERENCES connected_account(account_id)
            ON DELETE CASCADE,
    CONSTRAINT chk_account_transaction_sync_period
        CHECK (requested_start_date <= requested_end_date)
) COMMENT='계좌별 CODEF 거래내역 동기화 범위'
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;
