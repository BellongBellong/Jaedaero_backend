-- Apply once to an already-created local jaedaero database.
CREATE TABLE IF NOT EXISTS account_transaction_sync (
    sync_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '거래내역 조회 범위 동기화 ID',
    account_id BIGINT NOT NULL COMMENT '연동 계좌 ID',
    inquiry_type ENUM('DEMAND_DEPOSIT', 'INSTALLMENT_SAVINGS') NOT NULL COMMENT '조회 API 유형',
    requested_start_date DATE NOT NULL COMMENT 'CODEF 조회 시작일',
    requested_end_date DATE NOT NULL COMMENT 'CODEF 조회 종료일',
    synced_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '동기화 완료 시각',
    CONSTRAINT uq_account_transaction_sync_period
        UNIQUE (account_id, inquiry_type, requested_start_date, requested_end_date),
    CONSTRAINT fk_account_transaction_sync_account
        FOREIGN KEY (account_id) REFERENCES connected_account(account_id) ON DELETE CASCADE,
    CONSTRAINT chk_account_transaction_sync_period
        CHECK (requested_start_date <= requested_end_date)
) COMMENT='계좌 거래내역 동기화 범위'
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;
