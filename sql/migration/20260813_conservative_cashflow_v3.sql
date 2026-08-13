-- 보수적/잠재 자산 이중값을 단일 "전역 예상 자산"으로 통합하고,
-- 군적금·투자 총 원금 스냅샷을 cashflow_forecast에 추가한다.

ALTER TABLE cashflow_forecast
    ADD COLUMN soldier_saving_principal BIGINT NOT NULL DEFAULT 0
        COMMENT '군적금 총 원금(기존 잔액 + 전역까지 예상 신규 납입)'
        AFTER expected_investment_amount,
    ADD COLUMN investment_principal BIGINT NOT NULL DEFAULT 0
        COMMENT '투자 총 원금(연동 증권계좌 매입금액 또는 백필 추정치 + 전역까지 예상 신규 납입)'
        AFTER soldier_saving_principal;

-- V2 결과가 있는 행은 기존 잠재값을 V3 단일 최종값으로 승격한다.
UPDATE cashflow_forecast
SET expected_asset = potential_expected_asset
WHERE potential_expected_asset > 0;

ALTER TABLE cashflow_forecast
    DROP COLUMN potential_expected_asset,
    MODIFY COLUMN expected_asset BIGINT NOT NULL DEFAULT 0
        COMMENT '전역 예상 자산(현재 순자산 + 급여-소비 + 군적금 이자·매칭지원금 + 투자 예상수익)',
    MODIFY COLUMN calculation_policy_version VARCHAR(50) NOT NULL
        DEFAULT 'CONSERVATIVE_CASHFLOW_V3_20260813'
        COMMENT '캐시플로우 계산 정책 버전';

-- 기존 CHECK가 potential_expected_asset을 참조하므로 컬럼 삭제 전에 제거한다.
ALTER TABLE simulation
    DROP CHECK chk_simulation_detail_snapshot_complete;

UPDATE simulation
SET expected_asset = potential_expected_asset
WHERE potential_expected_asset IS NOT NULL;

ALTER TABLE simulation
    DROP COLUMN potential_expected_asset,
    MODIFY COLUMN expected_asset BIGINT NOT NULL
        COMMENT '전역 예상 자산(현재 순자산 + 급여-소비 + 군적금 이자·매칭지원금 + 투자 예상수익)',
    ADD CONSTRAINT chk_simulation_detail_snapshot_complete
        CHECK (
            (
                calculation_months IS NULL
                AND base_asset IS NULL
                AND expected_salary IS NULL
                AND expected_spending IS NULL
                AND soldier_saving_principal IS NULL
                AND soldier_saving_interest IS NULL
                AND government_matching_support IS NULL
                AND investment_principal IS NULL
                AND expected_investment_return IS NULL
                AND unallocated_principal IS NULL
                AND calculation_policy_version IS NULL
            )
            OR
            (
                calculation_months IS NOT NULL
                AND base_asset IS NOT NULL
                AND expected_salary IS NOT NULL
                AND expected_spending IS NOT NULL
                AND soldier_saving_principal IS NOT NULL
                AND soldier_saving_interest IS NOT NULL
                AND government_matching_support IS NOT NULL
                AND investment_principal IS NOT NULL
                AND expected_investment_return IS NOT NULL
                AND unallocated_principal IS NOT NULL
                AND calculation_policy_version IS NOT NULL
            )
        );
