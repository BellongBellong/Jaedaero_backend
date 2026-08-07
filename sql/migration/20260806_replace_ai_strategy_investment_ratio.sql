-- AI 추천과 전략 적용의 투자 원본값을 월급 대비 비율에서 월 원화 금액으로 전환한다.
-- 기존 비율은 생성 당시 기준 월급 스냅샷이 없어 정확한 금액으로 복원할 수 없다.
-- 과거 AI 추천은 0원, 과거 전략 적용 이력은 NULL(복원 불가)로 남긴다.

ALTER TABLE ai_recommended_scenario
    DROP CHECK chk_ai_recommended_scenario_investment_ratio;

ALTER TABLE ai_recommended_scenario
    ADD COLUMN monthly_investment_amount BIGINT NOT NULL DEFAULT 0
        COMMENT '추천 월 투자 배분액(원)'
        AFTER monthly_saving_amount,
    DROP COLUMN investment_ratio;

UPDATE ai_recommended_scenario
SET monthly_saving_amount = LEAST(GREATEST(monthly_saving_amount, 0), 550000),
    monthly_spending_amount = GREATEST(monthly_spending_amount, 0);

ALTER TABLE ai_recommended_scenario
    ALTER COLUMN monthly_investment_amount DROP DEFAULT,
    ADD CONSTRAINT chk_ai_recommended_scenario_monthly_amounts
        CHECK (
            monthly_spending_amount >= 0
            AND monthly_saving_amount BETWEEN 0 AND 550000
            AND monthly_investment_amount >= 0
        );

-- 과거 상세 응답도 신규 DTO로 역직렬화할 수 있도록 JSON 필드명을 함께 전환한다.
UPDATE ai_analysis
SET result_json = JSON_REMOVE(
    JSON_SET(result_json, '$.recommendedScenario.monthlyInvestmentAmount', 0),
    '$.recommendedScenario.investmentRatio'
)
WHERE JSON_CONTAINS_PATH(result_json, 'one', '$.recommendedScenario.investmentRatio');

ALTER TABLE strategy_application
    DROP CHECK chk_strategy_application_investment_ratio;

ALTER TABLE strategy_application
    ADD COLUMN applied_monthly_investment_amount BIGINT NULL
        COMMENT '적용 월 투자 배분액(원)'
        AFTER applied_monthly_saving_amount,
    DROP COLUMN applied_investment_ratio;

UPDATE strategy_application
SET applied_monthly_saving_amount =
        LEAST(GREATEST(applied_monthly_saving_amount, 0), 550000),
    applied_monthly_spending_amount = GREATEST(applied_monthly_spending_amount, 0)
WHERE applied_monthly_saving_amount IS NOT NULL
   OR applied_monthly_spending_amount IS NOT NULL;

ALTER TABLE strategy_application
    ADD CONSTRAINT chk_strategy_application_monthly_amounts
        CHECK (
            (applied_monthly_spending_amount IS NULL OR applied_monthly_spending_amount >= 0)
            AND (applied_monthly_saving_amount IS NULL OR applied_monthly_saving_amount BETWEEN 0 AND 550000)
            AND (applied_monthly_investment_amount IS NULL OR applied_monthly_investment_amount >= 0)
        );
