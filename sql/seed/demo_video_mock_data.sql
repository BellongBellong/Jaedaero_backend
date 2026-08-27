-- 제대로운 시연 영상용 목 데이터
-- 사용 전 @demo_user_id만 시연할 사용자 ID로 변경하세요.
-- 이 스크립트는 기존 실계좌·거래내역을 삭제하지 않고 `demo-video-*` 식별자의 데이터만 추가·갱신합니다.
-- 현재 날짜를 기준으로 활성 휴가 일정과 최근 거래내역을 만들어 휴가모드 시연에도 바로 사용할 수 있습니다.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET time_zone = '+09:00';

SET @demo_user_id = 78;
SET @demo_prefix = CONCAT('demo-video-', @demo_user_id);

START TRANSACTION;

-- 사용자의 CODEF 연결이 없을 때만 시연용 연결을 생성합니다.
INSERT INTO codef_connection (
    user_id, connected_id_encrypted, connected_id_hash, status, last_sync_at, last_sync_error_message
)
SELECT
    @demo_user_id,
    CONCAT('mock-encrypted-', @demo_prefix),
    SHA2(CONCAT(@demo_prefix, '-connection'), 256),
    'ACTIVE',
    NOW(),
    NULL
WHERE NOT EXISTS (
    SELECT 1 FROM codef_connection WHERE user_id = @demo_user_id
);

SELECT connection_id
INTO @demo_connection_id
FROM codef_connection
WHERE user_id = @demo_user_id;

-- 나라사랑 입출금 계좌, 장병내일준비적금 2개, 투자 계좌
INSERT INTO connected_account (
    connection_id, institution_code, institution_name,
    account_number_encrypted, account_number_hash, account_masked,
    business_type, account_type, account_role, product_name,
    current_balance, available_balance, account_opened_date, maturity_date,
    last_synced_at, status
)
VALUES
    (@demo_connection_id, '004', 'KB국민은행',
     CONCAT('mock-encrypted-', @demo_prefix, '-narasarang'), SHA2(CONCAT(@demo_prefix, '-narasarang'), 256), '****-1122',
     'BK', 'DEMAND_DEPOSIT', 'NARASARANG', '나라사랑 우대통장',
     1088900, 1088900, DATE_SUB(CURDATE(), INTERVAL 5 MONTH), NULL, NOW(), 'ACTIVE'),
    (@demo_connection_id, '088', '신한은행',
     CONCAT('mock-encrypted-', @demo_prefix, '-saving-shinhan'), SHA2(CONCAT(@demo_prefix, '-saving-shinhan'), 256), '****-3001',
     'BK', 'INSTALLMENT_SAVINGS', 'SOLDIER_SAVING', '장병내일준비적금',
     1200000, NULL, DATE_SUB(CURDATE(), INTERVAL 4 MONTH), DATE_ADD(CURDATE(), INTERVAL 16 MONTH), NOW(), 'ACTIVE'),
    (@demo_connection_id, '020', '우리은행',
     CONCAT('mock-encrypted-', @demo_prefix, '-saving-woori'), SHA2(CONCAT(@demo_prefix, '-saving-woori'), 256), '****-2501',
     'BK', 'INSTALLMENT_SAVINGS', 'SOLDIER_SAVING', '장병내일준비적금',
     1000000, NULL, DATE_SUB(CURDATE(), INTERVAL 4 MONTH), DATE_ADD(CURDATE(), INTERVAL 16 MONTH), NOW(), 'ACTIVE'),
    (@demo_connection_id, '0309', '미래에셋증권',
     CONCAT('mock-encrypted-', @demo_prefix, '-brokerage'), SHA2(CONCAT(@demo_prefix, '-brokerage'), 256), '****-7788',
     'ST', 'BROKERAGE', 'GENERAL', '종합매매계좌',
     850000, 850000, DATE_SUB(CURDATE(), INTERVAL 3 MONTH), NULL, NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE
    institution_name = VALUES(institution_name),
    account_masked = VALUES(account_masked),
    product_name = VALUES(product_name),
    current_balance = VALUES(current_balance),
    available_balance = VALUES(available_balance),
    maturity_date = VALUES(maturity_date),
    last_synced_at = VALUES(last_synced_at),
    status = 'ACTIVE';

SELECT account_id INTO @narasarang_account_id
FROM connected_account
WHERE connection_id = @demo_connection_id
  AND account_number_hash = SHA2(CONCAT(@demo_prefix, '-narasarang'), 256);

SELECT account_id INTO @shinhan_saving_account_id
FROM connected_account
WHERE connection_id = @demo_connection_id
  AND account_number_hash = SHA2(CONCAT(@demo_prefix, '-saving-shinhan'), 256);

SELECT account_id INTO @woori_saving_account_id
FROM connected_account
WHERE connection_id = @demo_connection_id
  AND account_number_hash = SHA2(CONCAT(@demo_prefix, '-saving-woori'), 256);

SELECT account_id INTO @brokerage_account_id
FROM connected_account
WHERE connection_id = @demo_connection_id
  AND account_number_hash = SHA2(CONCAT(@demo_prefix, '-brokerage'), 256);

INSERT INTO soldier_saving (
    user_id, account_id, bank_name, monthly_amount, interest_rate,
    government_support_expected, start_date, end_date
)
VALUES
    (@demo_user_id, @shinhan_saving_account_id, '신한은행', 300000, 5.00, 1200000,
     DATE_SUB(CURDATE(), INTERVAL 4 MONTH), DATE_ADD(CURDATE(), INTERVAL 16 MONTH)),
    (@demo_user_id, @woori_saving_account_id, '우리은행', 250000, 5.00, 1000000,
     DATE_SUB(CURDATE(), INTERVAL 4 MONTH), DATE_ADD(CURDATE(), INTERVAL 16 MONTH))
ON DUPLICATE KEY UPDATE
    monthly_amount = VALUES(monthly_amount),
    interest_rate = VALUES(interest_rate),
    government_support_expected = VALUES(government_support_expected),
    start_date = VALUES(start_date),
    end_date = VALUES(end_date);

-- 휴가모드: 실행일 전날부터 3일 뒤까지 활성화됩니다.
UPDATE leave_mode
SET start_date = DATE_SUB(CURDATE(), INTERVAL 1 DAY),
    end_date = DATE_ADD(CURDATE(), INTERVAL 3 DAY),
    is_leave_mode_enabled = TRUE,
    deleted_at = NULL,
    budget_amount = 250000
WHERE user_id = @demo_user_id
  AND event_name = '휴가';

INSERT INTO leave_mode (
    user_id, event_name, start_date, end_date, is_leave_mode_enabled, budget_amount
)
SELECT
    @demo_user_id, '휴가', DATE_SUB(CURDATE(), INTERVAL 1 DAY), DATE_ADD(CURDATE(), INTERVAL 3 DAY), TRUE, 250000
WHERE NOT EXISTS (
    SELECT 1
    FROM leave_mode
    WHERE user_id = @demo_user_id AND event_name = '휴가'
);

-- 최근 소비·급여·저축·투자 흐름. 적금/투자는 소비 카테고리에서 제외됩니다.
INSERT INTO transaction_history (
    account_id, transaction_datetime, amount, balance_after, transaction_type,
    category, category_source, transaction_description, external_transaction_key
)
VALUES
    (@narasarang_account_id, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 18 DAY), '09:00:00'), 900000, 900000, 'DEPOSIT', 'SALARY', 'RULE', '국군재정관리단 급여', SHA2(CONCAT(@demo_prefix, '-salary-1'), 256)),
    (@narasarang_account_id, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 17 DAY), '12:15:00'), 12800, 887200, 'WITHDRAW', 'FOOD', 'RULE', '체크카드·GS25 PX', SHA2(CONCAT(@demo_prefix, '-food-1'), 256)),
    (@narasarang_account_id, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 15 DAY), '09:05:00'), 550000, 337200, 'WITHDRAW', 'ASSET', 'RULE', '장병내일준비적금 자동이체', SHA2(CONCAT(@demo_prefix, '-saving-transfer-1'), 256)),
    (@narasarang_account_id, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 11 DAY), '09:00:00'), 900000, 1237200, 'DEPOSIT', 'SALARY', 'RULE', '국군재정관리단 급여', SHA2(CONCAT(@demo_prefix, '-salary-2'), 256)),
    (@narasarang_account_id, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 10 DAY), '15:10:00'), 4800, 1232400, 'WITHDRAW', 'CAFE', 'RULE', '체크카드·메가커피', SHA2(CONCAT(@demo_prefix, '-cafe-1'), 256)),
    (@narasarang_account_id, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '10:00:00'), 100000, 1132400, 'WITHDRAW', 'ASSET', 'RULE', '미래에셋증권 투자금 이체', SHA2(CONCAT(@demo_prefix, '-investment-transfer-1'), 256)),
    (@narasarang_account_id, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '18:20:00'), 3200, 1129200, 'WITHDRAW', 'TRANSPORT', 'RULE', '교통카드 충전', SHA2(CONCAT(@demo_prefix, '-transport-1'), 256)),
    (@narasarang_account_id, TIMESTAMP(CURDATE(), '13:10:00'), 9800, 1119400, 'WITHDRAW', 'FOOD', 'RULE', '체크카드·PX 간식', SHA2(CONCAT(@demo_prefix, '-food-2'), 256)),
    (@narasarang_account_id, TIMESTAMP(CURDATE(), '19:10:00'), 18500, 1100900, 'WITHDRAW', 'FOOD', 'RULE', '체크카드·휴가 저녁 식사', SHA2(CONCAT(@demo_prefix, '-vacation-food-1'), 256)),
    (@narasarang_account_id, TIMESTAMP(CURDATE(), '20:00:00'), 12000, 1088900, 'WITHDRAW', 'LEISURE', 'RULE', '체크카드·영화관', SHA2(CONCAT(@demo_prefix, '-vacation-leisure-1'), 256)),
    (@shinhan_saving_account_id, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 MONTH), '09:00:00'), 300000, 600000, 'DEPOSIT', NULL, NULL, '장병내일준비적금 3회차 납입', SHA2(CONCAT(@demo_prefix, '-shinhan-saving-3'), 256)),
    (@shinhan_saving_account_id, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 MONTH), '09:00:00'), 300000, 900000, 'DEPOSIT', NULL, NULL, '장병내일준비적금 4회차 납입', SHA2(CONCAT(@demo_prefix, '-shinhan-saving-4'), 256)),
    (@shinhan_saving_account_id, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 MONTH), '09:00:00'), 300000, 1200000, 'DEPOSIT', NULL, NULL, '장병내일준비적금 5회차 납입', SHA2(CONCAT(@demo_prefix, '-shinhan-saving-5'), 256)),
    (@woori_saving_account_id, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 MONTH), '09:00:00'), 250000, 500000, 'DEPOSIT', NULL, NULL, '장병내일준비적금 3회차 납입', SHA2(CONCAT(@demo_prefix, '-woori-saving-3'), 256)),
    (@woori_saving_account_id, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 MONTH), '09:00:00'), 250000, 750000, 'DEPOSIT', NULL, NULL, '장병내일준비적금 4회차 납입', SHA2(CONCAT(@demo_prefix, '-woori-saving-4'), 256)),
    (@woori_saving_account_id, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 MONTH), '09:00:00'), 250000, 1000000, 'DEPOSIT', NULL, NULL, '장병내일준비적금 5회차 납입', SHA2(CONCAT(@demo_prefix, '-woori-saving-5'), 256)),
    (@brokerage_account_id, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 8 DAY), '10:03:00'), 100000, 950000, 'DEPOSIT', 'ASSET', 'RULE', '증권계좌 투자금 입금', SHA2(CONCAT(@demo_prefix, '-brokerage-deposit'), 256)),
    (@brokerage_account_id, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 8 DAY), '10:05:00'), 100000, 850000, 'WITHDRAW', 'ASSET', 'RULE', 'KODEX 200 매수', SHA2(CONCAT(@demo_prefix, '-brokerage-buy'), 256))
ON DUPLICATE KEY UPDATE
    transaction_datetime = VALUES(transaction_datetime),
    amount = VALUES(amount),
    balance_after = VALUES(balance_after),
    transaction_type = VALUES(transaction_type),
    category = VALUES(category),
    category_source = VALUES(category_source),
    transaction_description = VALUES(transaction_description);

-- 통합 거래내역 API가 저장된 데이터만 반환하도록 최근 3개월 범위를 동기화 완료로 표시합니다.
INSERT INTO account_transaction_sync (
    account_id, inquiry_type, requested_start_date, requested_end_date
)
VALUES
    (@narasarang_account_id, 'DEMAND_DEPOSIT', DATE_SUB(CURDATE(), INTERVAL 3 MONTH), CURDATE()),
    (@shinhan_saving_account_id, 'INSTALLMENT_SAVINGS', DATE_SUB(CURDATE(), INTERVAL 3 MONTH), CURDATE()),
    (@woori_saving_account_id, 'INSTALLMENT_SAVINGS', DATE_SUB(CURDATE(), INTERVAL 3 MONTH), CURDATE())
ON DUPLICATE KEY UPDATE synced_at = CURRENT_TIMESTAMP;

INSERT INTO asset_snapshot (
    user_id, total_asset, total_saving, total_spending, snapshot_date
)
VALUES
    (@demo_user_id, 4138900, 2200000, 61100, CURDATE())
ON DUPLICATE KEY UPDATE
    total_asset = VALUES(total_asset),
    total_saving = VALUES(total_saving),
    total_spending = VALUES(total_spending);

COMMIT;

-- 확인용 조회
SELECT
    account.institution_name,
    account.product_name,
    account.account_masked,
    account.current_balance
FROM connected_account account
WHERE account.connection_id = @demo_connection_id
  AND account.account_masked IN ('****-1122', '****-3001', '****-2501', '****-7788');

SELECT event_name, start_date, end_date, budget_amount
FROM leave_mode
WHERE user_id = @demo_user_id AND event_name = '휴가' AND deleted_at IS NULL;
