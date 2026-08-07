-- 대시보드의 월 투자 실적과 적용된 월 투자 목표를 같은 단위로 비교하기 위해
-- 월별 캐시플로우에 계산 당시 투자 배분액을 저장한다.

ALTER TABLE cashflow_forecast_month
    ADD COLUMN expected_investment_amount BIGINT NOT NULL DEFAULT 0
        COMMENT '예상 월 투자 배분액'
        AFTER expected_saving_amount;
