-- AI Coach 개발용 Mock 데이터
--
-- 전제: jaedaero_db_v2.sql과 운영 기준 시드를 적용한 로컬 jaedaero DB
-- 이 파일은 DROP/TRUNCATE를 수행하지 않으며, 전용 소셜 식별자로 찾은 목업 데이터만 upsert한다.
-- 실행: mysql -u <DB_USER> -p jaedaero < sql/seed/ai_coach_mock_data.sql

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET time_zone = '+09:00';
START TRANSACTION;

SET @mock_social_id = 'mock-ai-coach-user-900001';

-- ---------------------------------------------------------------------------
-- 1. 사용자·복무정보·목표: simulations / investment guidance의 기본 입력
-- ---------------------------------------------------------------------------
INSERT INTO users (
    user_id, social_type, social_id, nickname, profile_image, profile_source, is_withdrawn
) VALUES (
    NULL, 'KAKAO', @mock_social_id, NULL, 'ARMY', 'OLIVE', FALSE
)
ON DUPLICATE KEY UPDATE
    user_id = LAST_INSERT_ID(user_id),
    profile_image = VALUES(profile_image),
    profile_source = VALUES(profile_source),
    is_withdrawn = FALSE,
    withdrawn_at = NULL;

SET @mock_user_id = LAST_INSERT_ID();

INSERT INTO soldier_profile (
    user_id, soldier_type, rank_name, enlistment_date, discharge_date, saving_join_yn
) VALUES (
    @mock_user_id, 'ARMY', '일병', '2026-03-01', '2027-09-01', TRUE
)
ON DUPLICATE KEY UPDATE
    soldier_type = VALUES(soldier_type),
    rank_name = VALUES(rank_name),
    enlistment_date = VALUES(enlistment_date),
    discharge_date = VALUES(discharge_date),
    saving_join_yn = VALUES(saving_join_yn);

INSERT INTO goal (user_id, target_amount, target_date, status)
VALUES (@mock_user_id, 20000000, '2027-09-01', 'ACTIVE')
ON DUPLICATE KEY UPDATE
    target_amount = VALUES(target_amount),
    target_date = VALUES(target_date),
    status = VALUES(status);

-- ---------------------------------------------------------------------------
-- 2. 계좌·적금·거래·자산: 캐시플로우 입력
-- ---------------------------------------------------------------------------
INSERT INTO codef_connection (
    connection_id, user_id, connected_id_encrypted, connected_id_hash, status, last_sync_at
) VALUES (
    NULL,
    @mock_user_id,
    'mock-encrypted-connected-id-900001',
    SHA2('mock-connected-id-900001', 256),
    'ACTIVE',
    '2026-07-31 09:00:00'
)
ON DUPLICATE KEY UPDATE
    connection_id = LAST_INSERT_ID(connection_id),
    status = 'ACTIVE',
    last_sync_at = VALUES(last_sync_at),
    last_sync_error_message = NULL;

SET @mock_connection_id = LAST_INSERT_ID();

INSERT INTO connected_account (
    account_id, connection_id, institution_code, institution_name,
    account_number_encrypted, account_number_hash, account_masked,
    account_type, account_role, product_name, current_balance, available_balance,
    account_opened_date, maturity_date, last_synced_at, status
) VALUES (
    NULL, @mock_connection_id, '004', '국민은행',
    'mock-encrypted-narasarang-900101', SHA2('mock-narasarang-900101', 256), '1234-****-9001',
    'DEMAND_DEPOSIT', 'NARASARANG', '나라사랑 우대통장', 1550000, 1550000,
    '2026-03-01', NULL, '2026-07-31 09:00:00', 'ACTIVE'
)
ON DUPLICATE KEY UPDATE
    account_id = LAST_INSERT_ID(account_id),
    current_balance = VALUES(current_balance),
    available_balance = VALUES(available_balance),
    last_synced_at = VALUES(last_synced_at),
    status = 'ACTIVE';

SET @mock_narasarang_account_id = LAST_INSERT_ID();

INSERT INTO connected_account (
    account_id, connection_id, institution_code, institution_name,
    account_number_encrypted, account_number_hash, account_masked,
    account_type, account_role, product_name, current_balance, available_balance,
    account_opened_date, maturity_date, last_synced_at, status
) VALUES (
    NULL, @mock_connection_id, '004', '국민은행',
    'mock-encrypted-soldier-saving-900102', SHA2('mock-soldier-saving-900102', 256), '5678-****-9002',
    'INSTALLMENT_SAVINGS', 'SOLDIER_SAVING', '장병내일준비적금', 2750000, NULL,
    '2026-03-01', '2027-09-01', '2026-07-31 09:00:00', 'ACTIVE'
)
ON DUPLICATE KEY UPDATE
    account_id = LAST_INSERT_ID(account_id),
    current_balance = VALUES(current_balance),
    available_balance = VALUES(available_balance),
    last_synced_at = VALUES(last_synced_at),
    status = 'ACTIVE';

SET @mock_saving_account_id = LAST_INSERT_ID();

-- 적립식 투자 가이드가 조회할 개발용 증권계좌와 내부 적립 계획이다.
INSERT INTO connected_account (
    account_id, connection_id, institution_code, institution_name,
    account_number_encrypted, account_number_hash, account_masked,
    business_type, account_type, account_role, product_name, current_balance, available_balance,
    account_opened_date, maturity_date, last_synced_at, status
) VALUES (
    NULL, @mock_connection_id, '0309', '미래에셋증권',
    'mock-encrypted-securities-900103', SHA2('mock-securities-900103', 256), '9001-****-9003',
    'ST', 'BROKERAGE', 'GENERAL', '종합매매계좌', 1100000, 100000,
    '2026-03-01', NULL, '2026-08-05 09:00:00', 'ACTIVE'
)
ON DUPLICATE KEY UPDATE
    account_id = LAST_INSERT_ID(account_id),
    business_type = VALUES(business_type),
    current_balance = VALUES(current_balance),
    available_balance = VALUES(available_balance),
    last_synced_at = VALUES(last_synced_at),
    status = VALUES(status);

SET @mock_securities_account_id = LAST_INSERT_ID();

INSERT INTO recurring_investment_plan (
    user_id, brokerage_account_id, frequency, contribution_day,
    contribution_amount, maximum_monthly_amount,
    investment_product_code, investment_product_name,
    status, next_contribution_date
) VALUES (
    @mock_user_id, @mock_securities_account_id, 'MONTHLY', 10,
    150000, 300000,
    '069500', 'KODEX 200',
    'ACTIVE', '2026-09-10'
)
ON DUPLICATE KEY UPDATE
    brokerage_account_id = VALUES(brokerage_account_id),
    frequency = VALUES(frequency),
    contribution_day = VALUES(contribution_day),
    contribution_amount = VALUES(contribution_amount),
    maximum_monthly_amount = VALUES(maximum_monthly_amount),
    investment_product_code = VALUES(investment_product_code),
    investment_product_name = VALUES(investment_product_name),
    status = VALUES(status),
    next_contribution_date = VALUES(next_contribution_date);

INSERT INTO soldier_saving (
    user_id, account_id, bank_name, monthly_amount, interest_rate,
    government_support_expected, start_date, end_date
) VALUES (
    @mock_user_id, @mock_saving_account_id, '국민은행', 550000, 5.00, 550000, '2026-03-01', '2027-09-01'
)
ON DUPLICATE KEY UPDATE
    monthly_amount = VALUES(monthly_amount),
    interest_rate = VALUES(interest_rate),
    government_support_expected = VALUES(government_support_expected),
    end_date = VALUES(end_date);

INSERT INTO transaction_history (
    account_id, transaction_datetime, amount, balance_after,
    transaction_type, category, category_source, transaction_description, external_transaction_key
) VALUES
    (@mock_narasarang_account_id, '2026-07-03 12:10:00', 12500, 1862500, 'WITHDRAW', '식비', 'RULE', 'PX', SHA2('mock-transaction-900001', 256)),
    (@mock_narasarang_account_id, '2026-07-04 18:10:00', 49900, 1812600, 'WITHDRAW', 'FOOD', 'RULE', '외식', SHA2('mock-transaction-900005', 256)),
    (@mock_narasarang_account_id, '2026-07-05 09:00:00', 17000, 1795600, 'WITHDRAW', 'LEISURE', 'AI', '넷플릭스', SHA2('mock-transaction-900006', 256)),
    (@mock_narasarang_account_id, '2026-07-05 20:10:00', 29300, 1766300, 'WITHDRAW', 'SHOPPING', 'RULE', '쿠팡', SHA2('mock-transaction-900012', 256)),
    (@mock_narasarang_account_id, '2026-07-06 09:00:00', 550000, 1245600, 'WITHDRAW', 'ASSET', 'USER', '장병내일준비적금 납입', SHA2('mock-transaction-900007', 256)),
    (@mock_narasarang_account_id, '2026-07-08 19:30:00', 48000, 1814500, 'WITHDRAW', '여가', 'AI', '외출 식사', SHA2('mock-transaction-900002', 256)),
    (@mock_narasarang_account_id, '2026-07-15 09:00:00', 900000, 2714500, 'DEPOSIT', NULL, NULL, '월급', SHA2('mock-transaction-900003', 256)),
    (@mock_narasarang_account_id, '2026-07-22 18:20:00', 1164500, 1550000, 'WITHDRAW', '저축', 'USER', '장병내일준비적금 및 저축 이체', SHA2('mock-transaction-900004', 256)),
    (@mock_narasarang_account_id, '2026-08-02 12:30:00', 82400, 1467600, 'WITHDRAW', 'FOOD', 'RULE', '외식', SHA2('mock-transaction-900008', 256)),
    (@mock_narasarang_account_id, '2026-08-04 09:00:00', 17000, 1450600, 'WITHDRAW', 'LEISURE', 'AI', '넷플릭스', SHA2('mock-transaction-900009', 256)),
    (@mock_narasarang_account_id, '2026-08-05 09:00:00', 550000, 900600, 'WITHDRAW', 'ASSET', 'USER', '장병내일준비적금 납입', SHA2('mock-transaction-900010', 256)),
    (@mock_narasarang_account_id, '2026-08-06 20:10:00', 29300, 871300, 'WITHDRAW', 'SHOPPING', 'RULE', '쿠팡', SHA2('mock-transaction-900011', 256))
ON DUPLICATE KEY UPDATE
    amount = VALUES(amount),
    balance_after = VALUES(balance_after),
    category = VALUES(category),
    category_source = VALUES(category_source),
    transaction_description = VALUES(transaction_description);

INSERT INTO asset_snapshot (
    user_id, total_asset, total_saving, total_spending, snapshot_date
) VALUES (
    @mock_user_id, 5400000, 2750000, 60500, '2026-07-31'
)
ON DUPLICATE KEY UPDATE
    total_asset = VALUES(total_asset),
    total_saving = VALUES(total_saving),
    total_spending = VALUES(total_spending);

-- ---------------------------------------------------------------------------
-- 3. 캐시플로우 결과: 평소 모드 AI 분석과 시뮬레이션 비교의 기준선
-- ---------------------------------------------------------------------------
INSERT INTO cashflow_forecast (
    user_id, base_asset, expected_salary, expected_saving_amount,
    expected_asset, monthly_spending_limit, achievement_rate,
    financial_discharge_date, policy_version, generated_at
)
SELECT
    @mock_user_id, 5400000, 17400000, 21268959,
    33618959, 350000, 168.09,
    '2027-08-05', 'mock-ai-coach-v2', '2026-08-13 09:00:00'
WHERE NOT EXISTS (
    SELECT 1
    FROM cashflow_forecast
    WHERE user_id = @mock_user_id
      AND policy_version = 'mock-ai-coach-v2'
);

UPDATE cashflow_forecast
SET
    base_asset = 5400000,
    expected_salary = 17400000,
    expected_saving_amount = 21268959,
    expected_asset = 33618959,
    monthly_spending_limit = 350000,
    achievement_rate = 168.09,
    financial_discharge_date = '2027-08-05',
    generated_at = '2026-08-13 09:00:00'
WHERE user_id = @mock_user_id
  AND policy_version = 'mock-ai-coach-v2';

SET @mock_forecast_id = (
    SELECT forecast_id
    FROM cashflow_forecast
    WHERE user_id = @mock_user_id
      AND policy_version = 'mock-ai-coach-v2'
    ORDER BY forecast_id DESC
    LIMIT 1
);

INSERT INTO cashflow_forecast_month (
    forecast_id, forecast_month, expected_rank, expected_salary,
    expected_saving_amount, expected_spending_amount, expected_ending_asset
) VALUES
    (@mock_forecast_id, '2026-08-01', '일병', 900000, 550000, 0, 6300000),
    (@mock_forecast_id, '2026-09-01', '일병', 900000, 550000, 0, 7200000),
    (@mock_forecast_id, '2026-10-01', '일병', 900000, 550000, 0, 8100000),
    (@mock_forecast_id, '2026-11-01', '상병', 1200000, 550000, 0, 9300000),
    (@mock_forecast_id, '2026-12-01', '상병', 1200000, 550000, 0, 10500000),
    (@mock_forecast_id, '2027-01-01', '상병', 1200000, 550000, 0, 11700000),
    (@mock_forecast_id, '2027-02-01', '상병', 1200000, 550000, 0, 12900000),
    (@mock_forecast_id, '2027-03-01', '상병', 1200000, 550000, 0, 14100000),
    (@mock_forecast_id, '2027-04-01', '상병', 1200000, 550000, 0, 15300000),
    (@mock_forecast_id, '2027-05-01', '병장', 1500000, 550000, 0, 16800000),
    (@mock_forecast_id, '2027-06-01', '병장', 1500000, 550000, 0, 18300000),
    (@mock_forecast_id, '2027-07-01', '병장', 1500000, 550000, 0, 19800000),
    (@mock_forecast_id, '2027-08-01', '병장', 1500000, 550000, 0, 21300000),
    (@mock_forecast_id, '2027-09-01', '병장', 1500000, 550000, 0, 33618959)
ON DUPLICATE KEY UPDATE
    expected_rank = VALUES(expected_rank),
    expected_salary = VALUES(expected_salary),
    expected_saving_amount = VALUES(expected_saving_amount),
    expected_spending_amount = VALUES(expected_spending_amount),
    expected_ending_asset = VALUES(expected_ending_asset);

DELETE FROM cashflow_forecast_month
WHERE forecast_id = @mock_forecast_id
  AND forecast_month NOT BETWEEN '2026-08-01' AND '2027-09-01';

COMMIT;
