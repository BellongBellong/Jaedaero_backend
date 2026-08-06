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
