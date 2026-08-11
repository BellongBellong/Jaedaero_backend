-- 기존 daily_market_report를 Gemini 기반 오늘의 AI 시장 리포트 계약으로 변환한다.
-- 이 파일은 현재 스키마에 한 번 적용하는 마이그레이션이며 반복 적용 스크립트가 아니다.
-- 기존 리포트·지표 행은 보존하고, 과거 OpenAI 생성 메타데이터는 출처를 오인하지 않도록 FALLBACK으로 변환한다.

UPDATE daily_market_report
SET generation_source = 'FALLBACK',
    model_name = 'legacy',
    prompt_version = 'legacy-market-report-v1'
WHERE generation_source = 'OPENAI';

UPDATE daily_market_report
SET generation_source = 'FALLBACK'
WHERE generation_source IS NULL;

UPDATE daily_market_report
SET model_name = 'legacy'
WHERE model_name IS NULL
   OR model_name = 'gpt-5-nano';

UPDATE daily_market_report
SET prompt_version = 'legacy-market-report-v1'
WHERE prompt_version IS NULL
   OR prompt_version LIKE '%openai%';

UPDATE daily_market_report
SET content =
        '기존 시장 리포트는 새 사실 기반 리포트 계약으로 재생성되기 전까지 안전한 대체 상태로 제공됩니다.'
WHERE content IS NULL OR content = '';

ALTER TABLE daily_market_report
    DROP COLUMN market_condition,
    MODIFY COLUMN content TEXT NOT NULL
        COMMENT '선별된 Finnhub 뉴스에 근거해 Gemini가 생성한 사실 기반 시장 리포트 본문',
    MODIFY COLUMN report_status ENUM('NORMAL', 'PARTIAL', 'STALE') NOT NULL DEFAULT 'NORMAL'
        COMMENT '리포트 전체 상태',
    MODIFY COLUMN generation_source ENUM('GEMINI', 'FALLBACK') NOT NULL
        COMMENT '본문 생성 경로 — Gemini 성공 또는 안전한 대체 상태',
    MODIFY COLUMN model_name VARCHAR(100) NOT NULL
        COMMENT '생성에 사용한 모델명(gemini-3.6-flash)',
    MODIFY COLUMN prompt_version VARCHAR(100) NOT NULL
        COMMENT 'Gemini Interactions 프롬프트 버전';

CREATE TABLE daily_market_report_source (
    source_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_id       BIGINT NOT NULL,
    source_order    SMALLINT UNSIGNED NOT NULL,
    title           VARCHAR(500) NOT NULL,
    url             VARCHAR(2048) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_daily_market_report_source_order
        UNIQUE (report_id, source_order),
    CONSTRAINT chk_daily_market_report_source_url
        CHECK (LOWER(url) REGEXP '^(http|https)://'),
    CONSTRAINT fk_daily_market_report_source_report
        FOREIGN KEY (report_id) REFERENCES daily_market_report(report_id)
            ON DELETE CASCADE
) COMMENT='오늘의 AI 시장 리포트 인용 출처 메타데이터 — 기사 전문은 저장하지 않음'
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;
