-- cashflow_forecast를 보수적 순자산과 미확정 예상 혜택으로 분리한다.
ALTER TABLE cashflow_forecast
    ADD COLUMN expected_spending BIGINT NOT NULL DEFAULT 0
        COMMENT '전역까지 예상 총소비' AFTER expected_salary,
    ADD COLUMN expected_investment_amount BIGINT NOT NULL DEFAULT 0
        COMMENT '전역까지 예상 투자 원금' AFTER expected_saving_amount,
    MODIFY COLUMN expected_saving_amount BIGINT NOT NULL DEFAULT 0
        COMMENT '전역까지 예상 군적금 납입 원금',
    MODIFY COLUMN expected_asset BIGINT NOT NULL DEFAULT 0
        COMMENT '미확정 혜택을 제외한 보수적 전역 예상 자산',
    ADD COLUMN soldier_saving_interest BIGINT NOT NULL DEFAULT 0
        COMMENT '군적금 예상 이자 참고값' AFTER expected_asset,
    ADD COLUMN government_matching_support BIGINT NOT NULL DEFAULT 0
        COMMENT '정부 매칭지원금 참고값' AFTER soldier_saving_interest,
    ADD COLUMN expected_investment_return BIGINT NOT NULL DEFAULT 0
        COMMENT '예상 투자수익 참고값' AFTER government_matching_support,
    ADD COLUMN projected_benefit_amount BIGINT NOT NULL DEFAULT 0
        COMMENT '미확정 예상 혜택 합계' AFTER expected_investment_return,
    ADD COLUMN potential_expected_asset BIGINT NOT NULL DEFAULT 0
        COMMENT '보수적 예상 자산과 예상 혜택 합계' AFTER projected_benefit_amount,
    ADD COLUMN calculation_policy_version VARCHAR(50) NULL
        COMMENT '캐시플로우 계산 정책 버전' AFTER potential_expected_asset;

-- 기존 결과는 이전 의미를 보존하고, 새 계산 결과부터 V2 정책 버전을 기록한다.
UPDATE cashflow_forecast
SET calculation_policy_version = 'LEGACY_BEFORE_CONSERVATIVE_V2'
WHERE calculation_policy_version IS NULL;

ALTER TABLE cashflow_forecast
    MODIFY COLUMN calculation_policy_version VARCHAR(50) NOT NULL
        DEFAULT 'LEGACY_BEFORE_CONSERVATIVE_V2'
        COMMENT '캐시플로우 계산 정책 버전';
