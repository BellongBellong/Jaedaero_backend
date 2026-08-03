-- ai_analysis 원본 문구 생성 경로 분리
--
-- OPENAI/FALLBACK만 저장한다. CACHE는 기존 OPENAI 결과를 재사용한
-- 이번 HTTP 응답의 상태이므로 DB에 저장하지 않는다.

ALTER TABLE ai_analysis
    ADD COLUMN generation_source ENUM('OPENAI', 'FALLBACK') NULL
        COMMENT '원본 문구 생성 경로 — OpenAI 성공 또는 템플릿 대체'
        AFTER prompt_version;

UPDATE ai_analysis
SET generation_source = CASE
        WHEN prompt_version LIKE '%-fallback' THEN 'FALLBACK'
        ELSE 'OPENAI'
    END
WHERE generation_source IS NULL;

UPDATE ai_analysis
SET prompt_version = LEFT(prompt_version, CHAR_LENGTH(prompt_version) - CHAR_LENGTH('-fallback'))
WHERE prompt_version LIKE '%-fallback';

ALTER TABLE ai_analysis
    MODIFY COLUMN generation_source ENUM('OPENAI', 'FALLBACK') NOT NULL
        COMMENT '원본 문구 생성 경로 — OpenAI 성공 또는 템플릿 대체';
