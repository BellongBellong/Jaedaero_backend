-- What-if 투자 배분 원본값을 월급 대비 비율에서 월 원화 금액으로 전환한다.
-- 기존 비율은 계산 당시 기준 월급 스냅샷이 없어 정확한 금액으로 복원할 수 없으므로
-- 과거 행의 월 투자금액은 0원으로 초기화한다.

ALTER TABLE simulation
    DROP CHECK chk_simulation_investment_ratio;

ALTER TABLE simulation
    ADD COLUMN monthly_investment_amount BIGINT NOT NULL DEFAULT 0
        COMMENT '군적금 외 월 투자 배분액(원)'
        AFTER monthly_saving_amount,
    DROP COLUMN investment_ratio;

ALTER TABLE simulation
    ALTER COLUMN monthly_investment_amount DROP DEFAULT,
    ADD CONSTRAINT chk_simulation_monthly_amounts
        CHECK (
            monthly_spending_amount >= 0
            AND monthly_saving_amount BETWEEN 0 AND 550000
            AND monthly_investment_amount >= 0
        );
