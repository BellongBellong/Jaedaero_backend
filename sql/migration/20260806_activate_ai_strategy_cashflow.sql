-- AI 추천 적용의 원본 분석을 직접 추적하고 동일 분석 재적용을 멱등 처리한다.
-- 기존 적용 행은 result_json과의 신뢰 가능한 FK 관계가 없어 analysis_id를 NULL로 유지한다.

ALTER TABLE strategy_application
    ADD COLUMN analysis_id BIGINT NULL
        COMMENT '원본 AI 분석 ID — AI 추천 적용 멱등성 키'
        AFTER source_type,
    ADD CONSTRAINT fk_strategy_application_analysis
        FOREIGN KEY (analysis_id) REFERENCES ai_analysis(analysis_id)
            ON DELETE SET NULL,
    ADD CONSTRAINT uq_strategy_application_ai_analysis
        UNIQUE (user_id, analysis_id);
