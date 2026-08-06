-- What-if 계산 상세 스냅샷을 저장해 이력/상세 조회에서도 실행 당시 근거를 재현한다.
-- 기존 simulation 행은 상세값을 신뢰할 수 없어 NULL로 유지한다.

ALTER TABLE simulation
    ADD COLUMN calculation_months INT NULL COMMENT '상세 계산에 포함한 개월 수' AFTER financial_discharge_date,
    ADD COLUMN base_asset BIGINT NULL COMMENT '계산 시점 현재 자산 스냅샷' AFTER calculation_months,
    ADD COLUMN expected_salary BIGINT NULL COMMENT '계산 기간 예상 급여 합계' AFTER base_asset,
    ADD COLUMN expected_spending BIGINT NULL COMMENT '계산 기간 예상 소비 합계' AFTER expected_salary,
    ADD COLUMN soldier_saving_principal BIGINT NULL COMMENT '계산 기간 장병내일준비적금 납입 원금' AFTER expected_spending,
    ADD COLUMN soldier_saving_interest BIGINT NULL COMMENT '연 5% 월복리 가정 예상 이자' AFTER soldier_saving_principal,
    ADD COLUMN government_matching_support BIGINT NULL COMMENT '군적금 미래 납입원금의 100% 매칭지원금 가정' AFTER soldier_saving_interest,
    ADD COLUMN investment_principal BIGINT NULL COMMENT '계산 기간 투자 원금' AFTER government_matching_support,
    ADD COLUMN expected_investment_return BIGINT NULL COMMENT '월복리 가정 예상 투자수익' AFTER investment_principal,
    ADD COLUMN unallocated_principal BIGINT NULL COMMENT '급여에서 소비·군적금·투자 후 남는 금액 합계' AFTER expected_investment_return,
    ADD COLUMN potential_expected_asset BIGINT NULL COMMENT '예상 혜택 실현 시 참고 예상자산' AFTER unallocated_principal,
    ADD COLUMN calculation_policy_version VARCHAR(50) NULL COMMENT '상세 계산 정책 버전' AFTER potential_expected_asset;

-- 위 ALTER 직후 기존 행은 신규 컬럼이 모두 NULL이고, 신규 애플리케이션 행은 전체 값을 저장한다.
-- 부분 스냅샷은 조회 DTO의 완전성 가정을 깨므로 DB에서도 차단한다.
ALTER TABLE simulation
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
                AND potential_expected_asset IS NULL
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
                AND potential_expected_asset IS NOT NULL
                AND calculation_policy_version IS NOT NULL
            )
        );
