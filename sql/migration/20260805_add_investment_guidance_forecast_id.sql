-- 기존 DB의 투자 가이드가 계산에 사용한 캐시플로우 버전을 추적하도록 보강한다.
-- 신규 설치는 jaedaero_db_v1.sql만 적용하면 된다.

ALTER TABLE investment_guidance
    ADD COLUMN forecast_id BIGINT NULL COMMENT '계산에 사용한 캐시플로우 예측 ID'
        AFTER plan_id;

UPDATE investment_guidance guidance
SET forecast_id = (
    SELECT forecast.forecast_id
    FROM cashflow_forecast forecast
    WHERE forecast.user_id = guidance.user_id
      AND forecast.generated_at <= guidance.created_at
    ORDER BY forecast.generated_at DESC, forecast.forecast_id DESC
    LIMIT 1
)
WHERE guidance.forecast_id IS NULL;

-- 타임스탬프 정밀도 차이로 이전 예측을 못 찾은 경우 사용자의 최신 예측으로 보완한다.
UPDATE investment_guidance guidance
SET forecast_id = (
    SELECT forecast.forecast_id
    FROM cashflow_forecast forecast
    WHERE forecast.user_id = guidance.user_id
    ORDER BY forecast.generated_at DESC, forecast.forecast_id DESC
    LIMIT 1
)
WHERE guidance.forecast_id IS NULL;

ALTER TABLE investment_guidance
    MODIFY COLUMN forecast_id BIGINT NOT NULL COMMENT '계산에 사용한 캐시플로우 예측 ID',
    ADD CONSTRAINT fk_investment_guidance_forecast
        FOREIGN KEY (forecast_id) REFERENCES cashflow_forecast(forecast_id)
            ON DELETE RESTRICT;
