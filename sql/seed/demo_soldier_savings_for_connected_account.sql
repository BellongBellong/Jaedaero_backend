-- 시연용 거래내역 + 장병내일준비적금 2개를 추가합니다.
-- 기존 CODEF 연동 입출금 계좌는 유지하고, 국민 나라사랑 입출금 계좌를 출금 계좌로 자동 선택합니다.
-- 2026-05-01 입대, 육군 기준: 6월 이병(75만 원), 7~8월 일병(90만 원) 급여 흐름입니다.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET @demo_user_id = 1548561;
SET @demo_prefix = CONCAT('demo-soldier-saving-', @demo_user_id);
SET @demo_start_date = '2026-06-01';
SET @demo_end_date = CURDATE();
SET @enlistment_date = '2026-05-01';

START TRANSACTION;

-- 연결 상태가 ERROR여도 활성 계좌가 남아 있으면 시연 대상으로 사용합니다.
-- CODEF 데모 연결은 계좌 적재 후 상태가 ERROR로 남을 수 있습니다.
SELECT cc.connection_id
INTO @demo_connection_id
FROM codef_connection cc
WHERE cc.user_id = @demo_user_id
  AND EXISTS (
      SELECT 1
      FROM connected_account ca
      WHERE ca.connection_id = cc.connection_id
        AND ca.status = 'ACTIVE'
  )
ORDER BY CASE WHEN cc.status = 'ACTIVE' THEN 0 ELSE 1 END, cc.connection_id DESC
LIMIT 1;

-- 활성 상태의 국민 나라사랑 입출금 계좌를 우선 선택하고, 없으면 다른 국민 입출금 계좌를 사용합니다.
SELECT account_id
INTO @kb_demand_account_id
FROM connected_account
WHERE connection_id = @demo_connection_id
  AND institution_code = '0004'
  AND business_type = 'BK'
  AND account_type = 'DEMAND_DEPOSIT'
  AND status = 'ACTIVE'
ORDER BY CASE WHEN product_name LIKE '%나라사랑%' THEN 0 ELSE 1 END, account_id DESC
LIMIT 1;

-- 국민은행 월 30만 원, 하나은행 월 25만 원 장병내일준비적금
INSERT INTO connected_account (
    connection_id, institution_code, institution_name,
    account_number_encrypted, account_number_hash, account_masked,
    business_type, account_type, account_role, product_name,
    current_balance, available_balance, account_opened_date, maturity_date,
    last_synced_at, status
)
VALUES
    (@demo_connection_id, '0004', 'KB국민은행',
     CONCAT('demo-encrypted-', @demo_prefix, '-kb-300'),
     SHA2('DEMO-KB-SAVING-3001', 256), '****-3001',
     'BK', 'INSTALLMENT_SAVINGS', 'SOLDIER_SAVING', '장병내일준비적금',
     900000, NULL, @enlistment_date, '2027-11-01', NOW(), 'ACTIVE'),
    (@demo_connection_id, '0081', '하나은행',
     CONCAT('demo-encrypted-', @demo_prefix, '-hana-250'),
     SHA2('DEMO-HANA-SAVING-2501', 256), '****-2501',
     'BK', 'INSTALLMENT_SAVINGS', 'SOLDIER_SAVING', '장병내일준비적금',
     750000, NULL, @enlistment_date, '2027-11-01', NOW(), 'ACTIVE')
ON DUPLICATE KEY UPDATE
    institution_name = VALUES(institution_name),
    account_masked = VALUES(account_masked),
    account_type = VALUES(account_type),
    account_role = VALUES(account_role),
    product_name = VALUES(product_name),
    current_balance = VALUES(current_balance),
    maturity_date = VALUES(maturity_date),
    last_synced_at = VALUES(last_synced_at),
    status = 'ACTIVE';

SELECT account_id INTO @kb_saving_account_id
FROM connected_account
WHERE connection_id = @demo_connection_id
  AND account_number_hash = SHA2('DEMO-KB-SAVING-3001', 256);

SELECT account_id INTO @hana_saving_account_id
FROM connected_account
WHERE connection_id = @demo_connection_id
  AND account_number_hash = SHA2('DEMO-HANA-SAVING-2501', 256);

INSERT INTO soldier_saving (
    user_id, account_id, source_type, bank_name, monthly_amount, interest_rate,
    government_support_expected, start_date, end_date
)
VALUES
    (@demo_user_id, @kb_saving_account_id, 'DEMO', 'KB국민은행', 300000, 5.00,
     900000, @enlistment_date, '2027-11-01'),
    (@demo_user_id, @hana_saving_account_id, 'DEMO', '하나은행', 250000, 5.00,
     750000, @enlistment_date, '2027-11-01')
ON DUPLICATE KEY UPDATE
    source_type = 'DEMO',
    bank_name = VALUES(bank_name),
    monthly_amount = VALUES(monthly_amount),
    interest_rate = VALUES(interest_rate),
    government_support_expected = VALUES(government_support_expected),
    start_date = VALUES(start_date),
    end_date = VALUES(end_date);

-- 기존 시연 데이터의 8월 중복 급여는 제거해 월별 급여가 한 번만 집계되도록 합니다.
DELETE FROM transaction_history
WHERE account_id = @kb_demand_account_id
  AND transaction_datetime = '2026-08-12 09:00:00'
  AND transaction_description = '국군재정관리단 급여';

-- 휴가 기간에는 PX 결제가 보이지 않도록 기존 시연 PX 거래를 제거합니다.
-- 체크카드 외부 소비와 전자금융 이체는 그대로 유지합니다.
DELETE FROM transaction_history
WHERE account_id = @kb_demand_account_id
  AND DATE(transaction_datetime) BETWEEN '2026-08-23' AND '2026-08-27'
  AND transaction_description LIKE '%PX%';

-- 이전 demo-video 시드가 이 사용자에게 만든 국민 입출금 거래를 정리합니다.
-- 외부 CODEF 거래는 키가 다르므로 삭제하지 않습니다.
DELETE FROM transaction_history
WHERE account_id = @kb_demand_account_id
  AND external_transaction_key IN (
      SHA2(CONCAT('demo-video-', @demo_user_id, '-salary-1'), 256),
      SHA2(CONCAT('demo-video-', @demo_user_id, '-food-1'), 256),
      SHA2(CONCAT('demo-video-', @demo_user_id, '-saving-transfer-1'), 256),
      SHA2(CONCAT('demo-video-', @demo_user_id, '-salary-2'), 256),
      SHA2(CONCAT('demo-video-', @demo_user_id, '-cafe-1'), 256),
      SHA2(CONCAT('demo-video-', @demo_user_id, '-investment-transfer-1'), 256),
      SHA2(CONCAT('demo-video-', @demo_user_id, '-transport-1'), 256),
      SHA2(CONCAT('demo-video-', @demo_user_id, '-food-2'), 256),
      SHA2(CONCAT('demo-video-', @demo_user_id, '-vacation-food-1'), 256),
      SHA2(CONCAT('demo-video-', @demo_user_id, '-vacation-leisure-1'), 256)
  );

-- 이전 버전 시드가 만든 단일 납입 내역만 제거합니다. 실제 CODEF 거래내역은 삭제하지 않습니다.
DELETE FROM transaction_history
WHERE (account_id = @kb_saving_account_id
       AND external_transaction_key = SHA2(CONCAT(@demo_prefix, '-kb-payment'), 256))
   OR (account_id = @hana_saving_account_id
       AND external_transaction_key = SHA2(CONCAT(@demo_prefix, '-hana-payment'), 256));

-- 급여, 적금 이체, PX·휴가 소비, 구독료.
-- 모든 소비 거래는 실제 카드 승인 적요처럼 '체크카드'로 시작시켜 소비 분석 대상에 포함됩니다.
INSERT INTO transaction_history (
    account_id, transaction_datetime, amount, balance_after, transaction_type,
    category, category_source, transaction_description, external_transaction_key
)
VALUES
    -- 월급: 2026-05-01 입대 기준 6월 이병, 7~8월 일병
    (@kb_demand_account_id, '2026-06-10 09:00:00', 750000, NULL, 'DEPOSIT', 'SALARY', 'RULE', '오픈뱅킹입금·국군재정단', SHA2(CONCAT(@demo_prefix, '-salary-202606'), 256)),
    (@kb_demand_account_id, '2026-07-10 09:00:00', 900000, NULL, 'DEPOSIT', 'SALARY', 'RULE', '오픈뱅킹입금·국군재정단', SHA2(CONCAT(@demo_prefix, '-salary-202607'), 256)),
    (@kb_demand_account_id, '2026-08-10 09:00:00', 900000, NULL, 'DEPOSIT', 'SALARY', 'RULE', '오픈뱅킹입금·국군재정단', SHA2(CONCAT(@demo_prefix, '-salary-202608'), 256)),

    -- 매월 11일 적금 자동이체: 국민 30만 원 + 하나 25만 원
    (@kb_demand_account_id, '2026-06-11 09:10:00', 300000, NULL, 'WITHDRAW', 'ASSET', 'RULE', '장병내일준비적금 자동이체·KB국민은행', SHA2(CONCAT(@demo_prefix, '-kb-transfer-202606'), 256)),
    (@kb_saving_account_id, '2026-06-11 09:11:00', 300000, 300000, 'DEPOSIT', 'ASSET', 'RULE', '장병내일준비적금 1회차 납입', SHA2(CONCAT(@demo_prefix, '-kb-saving-202606'), 256)),
    (@kb_demand_account_id, '2026-06-11 09:12:00', 250000, NULL, 'WITHDRAW', 'ASSET', 'RULE', '장병내일준비적금 자동이체·하나은행', SHA2(CONCAT(@demo_prefix, '-hana-transfer-202606'), 256)),
    (@hana_saving_account_id, '2026-06-11 09:13:00', 250000, 250000, 'DEPOSIT', 'ASSET', 'RULE', '장병내일준비적금 1회차 납입', SHA2(CONCAT(@demo_prefix, '-hana-saving-202606'), 256)),
    (@kb_demand_account_id, '2026-07-11 09:10:00', 300000, NULL, 'WITHDRAW', 'ASSET', 'RULE', '장병내일준비적금 자동이체·KB국민은행', SHA2(CONCAT(@demo_prefix, '-kb-transfer-202607'), 256)),
    (@kb_saving_account_id, '2026-07-11 09:11:00', 300000, 600000, 'DEPOSIT', 'ASSET', 'RULE', '장병내일준비적금 2회차 납입', SHA2(CONCAT(@demo_prefix, '-kb-saving-202607'), 256)),
    (@kb_demand_account_id, '2026-07-11 09:12:00', 250000, NULL, 'WITHDRAW', 'ASSET', 'RULE', '장병내일준비적금 자동이체·하나은행', SHA2(CONCAT(@demo_prefix, '-hana-transfer-202607'), 256)),
    (@hana_saving_account_id, '2026-07-11 09:13:00', 250000, 500000, 'DEPOSIT', 'ASSET', 'RULE', '장병내일준비적금 2회차 납입', SHA2(CONCAT(@demo_prefix, '-hana-saving-202607'), 256)),
    (@kb_demand_account_id, '2026-08-11 09:10:00', 300000, NULL, 'WITHDRAW', 'ASSET', 'RULE', '장병내일준비적금 자동이체·KB국민은행', SHA2(CONCAT(@demo_prefix, '-kb-transfer-202608'), 256)),
    (@kb_saving_account_id, '2026-08-11 09:11:00', 300000, 900000, 'DEPOSIT', 'ASSET', 'RULE', '장병내일준비적금 3회차 납입', SHA2(CONCAT(@demo_prefix, '-kb-saving-202608'), 256)),
    (@kb_demand_account_id, '2026-08-11 09:12:00', 250000, NULL, 'WITHDRAW', 'ASSET', 'RULE', '장병내일준비적금 자동이체·하나은행', SHA2(CONCAT(@demo_prefix, '-hana-transfer-202608'), 256)),
    (@hana_saving_account_id, '2026-08-11 09:13:00', 250000, 750000, 'DEPOSIT', 'ASSET', 'RULE', '장병내일준비적금 3회차 납입', SHA2(CONCAT(@demo_prefix, '-hana-saving-202608'), 256)),

    -- 매월 구독료: 티빙 베이직 월 9,500원
    (@kb_demand_account_id, '2026-06-07 02:00:00', 9500, NULL, 'WITHDRAW', 'LEISURE', 'RULE', '체크카드·티빙 베이직 정기결제', SHA2(CONCAT(@demo_prefix, '-tving-202606'), 256)),
    (@kb_demand_account_id, '2026-07-07 02:00:00', 9500, NULL, 'WITHDRAW', 'LEISURE', 'RULE', '체크카드·티빙 베이직 정기결제', SHA2(CONCAT(@demo_prefix, '-tving-202607'), 256)),
    (@kb_demand_account_id, '2026-08-07 02:00:00', 9500, NULL, 'WITHDRAW', 'LEISURE', 'RULE', '체크카드·티빙 베이직 정기결제', SHA2(CONCAT(@demo_prefix, '-tving-202608'), 256)),
    (@kb_demand_account_id, '2026-06-03 02:00:00', 14900, NULL, 'WITHDRAW', 'LEISURE', 'RULE', '체크카드·유튜브 프리미엄 정기결제', SHA2(CONCAT(@demo_prefix, '-youtube-premium-202606'), 256)),
    (@kb_demand_account_id, '2026-07-03 02:00:00', 14900, NULL, 'WITHDRAW', 'LEISURE', 'RULE', '체크카드·유튜브 프리미엄 정기결제', SHA2(CONCAT(@demo_prefix, '-youtube-premium-202607'), 256)),
    (@kb_demand_account_id, '2026-08-03 02:00:00', 14900, NULL, 'WITHDRAW', 'LEISURE', 'RULE', '체크카드·유튜브 프리미엄 정기결제', SHA2(CONCAT(@demo_prefix, '-youtube-premium-202608'), 256)),

    -- 부대 PX: 17~21시 사이 결제, 휴가 기간(7/10~7/15)에는 넣지 않음
    (@kb_demand_account_id, '2026-06-11 18:22:00', 4200, NULL, 'WITHDRAW', 'PX', 'RULE', '체크카드·국군복지단', SHA2(CONCAT(@demo_prefix, '-px-20260611'), 256)),
    (@kb_demand_account_id, '2026-06-14 19:14:00', 6800, NULL, 'WITHDRAW', 'PX', 'RULE', '체크카드·국군복지단', SHA2(CONCAT(@demo_prefix, '-px-20260614'), 256)),
    (@kb_demand_account_id, '2026-06-18 17:42:00', 3900, NULL, 'WITHDRAW', 'PX', 'RULE', '체크카드·국군복지단', SHA2(CONCAT(@demo_prefix, '-px-20260618'), 256)),
    (@kb_demand_account_id, '2026-06-23 20:05:00', 5300, NULL, 'WITHDRAW', 'PX', 'RULE', '체크카드·국군복지단', SHA2(CONCAT(@demo_prefix, '-px-20260623'), 256)),
    (@kb_demand_account_id, '2026-06-28 18:47:00', 7200, NULL, 'WITHDRAW', 'PX', 'RULE', '체크카드·국군복지단', SHA2(CONCAT(@demo_prefix, '-px-20260628'), 256)),
    (@kb_demand_account_id, '2026-07-02 18:36:00', 4600, NULL, 'WITHDRAW', 'PX', 'RULE', '체크카드·국군복지단', SHA2(CONCAT(@demo_prefix, '-px-20260702'), 256)),
    (@kb_demand_account_id, '2026-07-06 20:10:00', 8100, NULL, 'WITHDRAW', 'PX', 'RULE', '체크카드·국군복지단', SHA2(CONCAT(@demo_prefix, '-px-20260706'), 256)),
    (@kb_demand_account_id, '2026-07-17 19:22:00', 5300, NULL, 'WITHDRAW', 'PX', 'RULE', '체크카드·국군복지단', SHA2(CONCAT(@demo_prefix, '-px-20260717'), 256)),
    (@kb_demand_account_id, '2026-07-20 18:08:00', 6100, NULL, 'WITHDRAW', 'PX', 'RULE', '체크카드·국군복지단', SHA2(CONCAT(@demo_prefix, '-px-20260720'), 256)),
    (@kb_demand_account_id, '2026-07-24 20:41:00', 4300, NULL, 'WITHDRAW', 'PX', 'RULE', '체크카드·국군복지단', SHA2(CONCAT(@demo_prefix, '-px-20260724'), 256)),
    (@kb_demand_account_id, '2026-07-28 17:55:00', 7900, NULL, 'WITHDRAW', 'PX', 'RULE', '체크카드·국군복지단', SHA2(CONCAT(@demo_prefix, '-px-20260728'), 256)),
    (@kb_demand_account_id, '2026-08-01 19:06:00', 5500, NULL, 'WITHDRAW', 'PX', 'RULE', '체크카드·국군복지단', SHA2(CONCAT(@demo_prefix, '-px-20260801'), 256)),
    (@kb_demand_account_id, '2026-08-04 18:18:00', 4700, NULL, 'WITHDRAW', 'PX', 'RULE', '체크카드·국군복지단', SHA2(CONCAT(@demo_prefix, '-px-20260804'), 256)),
    (@kb_demand_account_id, '2026-08-08 20:25:00', 6900, NULL, 'WITHDRAW', 'PX', 'RULE', '체크카드·국군복지단', SHA2(CONCAT(@demo_prefix, '-px-20260808'), 256)),
    (@kb_demand_account_id, '2026-08-12 17:34:00', 8800, NULL, 'WITHDRAW', 'PX', 'RULE', '체크카드·국군복지단', SHA2(CONCAT(@demo_prefix, '-px-20260812'), 256)),
    (@kb_demand_account_id, '2026-08-16 19:49:00', 3600, NULL, 'WITHDRAW', 'PX', 'RULE', '체크카드·국군복지단', SHA2(CONCAT(@demo_prefix, '-px-20260816'), 256)),
    (@kb_demand_account_id, '2026-08-19 18:57:00', 5200, NULL, 'WITHDRAW', 'PX', 'RULE', '체크카드·국군복지단', SHA2(CONCAT(@demo_prefix, '-px-20260819'), 256)),
    (@kb_demand_account_id, '2026-08-22 20:12:00', 7600, NULL, 'WITHDRAW', 'PX', 'RULE', '체크카드·국군복지단', SHA2(CONCAT(@demo_prefix, '-px-20260822'), 256)),

    -- 휴가 2주 전 왕복 기차표, 의류 쇼핑
    (@kb_demand_account_id, '2026-08-09 14:06:00', 59800, NULL, 'WITHDRAW', 'TRANSPORT', 'RULE', '체크카드·코레일톡 KTX 왕복 승차권', SHA2(CONCAT(@demo_prefix, '-ktx-round-trip-20260809'), 256)),
    (@kb_demand_account_id, '2026-08-16 20:03:00', 89900, NULL, 'WITHDRAW', 'SHOPPING', 'RULE', '체크카드·무신사 스탠다드 의류', SHA2(CONCAT(@demo_prefix, '-musinsa-20260816'), 256)),

    -- 휴가: 8/23~8/27. 이 기간의 체크카드 지출은 휴가 지출 내역에 포함됩니다.
    (@kb_demand_account_id, '2026-08-23 18:26:00', 15400, NULL, 'WITHDRAW', 'FOOD', 'RULE', '체크카드·맥도날드 서울역점', SHA2(CONCAT(@demo_prefix, '-leave-20260823-food'), 256)),
    (@kb_demand_account_id, '2026-08-23 20:14:00', 6200, NULL, 'WITHDRAW', 'FOOD', 'RULE', '체크카드·스타벅스 서울역점', SHA2(CONCAT(@demo_prefix, '-leave-20260823-cafe'), 256)),
    (@kb_demand_account_id, '2026-08-24 15:18:00', 24900, NULL, 'WITHDRAW', 'SHOPPING', 'RULE', '체크카드·올리브영 홍대점', SHA2(CONCAT(@demo_prefix, '-leave-20260824-shopping'), 256)),
    -- 전자금융 이체는 소비 지표에 포함되지 않도록 체크카드 적요를 사용하지 않습니다.
    (@kb_demand_account_id, '2026-08-24 18:35:00', 45000, NULL, 'WITHDRAW', 'ETC', 'RULE', '전자금융·김민수', SHA2(CONCAT(@demo_prefix, '-leave-20260824-friend-transfer'), 256))
ON DUPLICATE KEY UPDATE
    transaction_datetime = VALUES(transaction_datetime),
    amount = VALUES(amount),
    balance_after = VALUES(balance_after),
    transaction_type = VALUES(transaction_type),
    category = VALUES(category),
    category_source = VALUES(category_source),
    transaction_description = VALUES(transaction_description);

-- PX 결제 적요를 실제 카드 내역 형식으로 통일합니다.
UPDATE transaction_history
SET transaction_description = '체크카드·국군복지단',
    category = 'PX',
    category_source = 'RULE'
WHERE account_id = @kb_demand_account_id
  AND (category = 'PX' OR transaction_description LIKE '%PX%');

-- 시연 화면에서 보이는 국민 입출금 계좌의 최종 잔액입니다.
-- 급여·적금 이체·소비 흐름을 반영한 데모 잔액으로만 갱신합니다.
UPDATE connected_account
SET current_balance = 779600,
    available_balance = 779600,
    last_synced_at = NOW()
WHERE account_id = @kb_demand_account_id;

-- 휴가모드: 8/23~8/27 동안의 거래는 휴가 지출 조회에 사용됩니다.
UPDATE leave_mode
SET start_date = '2026-08-23',
    end_date = '2026-08-27',
    is_leave_mode_enabled = TRUE,
    deleted_at = NULL,
    budget_amount = 250000
WHERE leave_mode_id = (
    SELECT leave_mode_id
    FROM (
        SELECT leave_mode_id
        FROM leave_mode
        WHERE user_id = @demo_user_id
          AND event_name = '휴가'
        ORDER BY leave_mode_id DESC
        LIMIT 1
    ) AS latest_leave_mode
);

INSERT INTO leave_mode (
    user_id, event_name, start_date, end_date, is_leave_mode_enabled, budget_amount
)
SELECT @demo_user_id, '휴가', '2026-08-23', '2026-08-27', TRUE, 250000
WHERE NOT EXISTS (
    SELECT 1
    FROM leave_mode
    WHERE user_id = @demo_user_id
      AND event_name = '휴가'
);

-- 저장된 시연 거래를 조회만 하도록 거래내역 조회 범위를 기록합니다.
INSERT INTO account_transaction_sync (
    account_id, inquiry_type, requested_start_date, requested_end_date
)
VALUES
    (@kb_demand_account_id, 'DEMAND_DEPOSIT', @demo_start_date, @demo_end_date),
    (@kb_saving_account_id, 'INSTALLMENT_SAVINGS', @demo_start_date, @demo_end_date),
    (@hana_saving_account_id, 'INSTALLMENT_SAVINGS', @demo_start_date, @demo_end_date)
ON DUPLICATE KEY UPDATE
    requested_start_date = VALUES(requested_start_date),
    requested_end_date = VALUES(requested_end_date),
    synced_at = CURRENT_TIMESTAMP;

COMMIT;

SELECT account_id, institution_name, product_name, account_masked, current_balance
FROM connected_account
WHERE account_id IN (@kb_demand_account_id, @kb_saving_account_id, @hana_saving_account_id)
ORDER BY account_id;

SELECT DATE(transaction_datetime) AS transaction_date, transaction_description, amount, transaction_type, category
FROM transaction_history
WHERE account_id IN (@kb_demand_account_id, @kb_saving_account_id, @hana_saving_account_id)
  AND transaction_datetime BETWEEN TIMESTAMP(@demo_start_date, '00:00:00')
                               AND TIMESTAMP(@demo_end_date, '23:59:59')
ORDER BY transaction_datetime;
