-- 기존 daily_market_report를 구조화 일일 리포트 계약으로 확장한다.
-- 기존 리포트는 생성 근거가 없으므로 NEUTRAL/FALLBACK/legacy 메타데이터로 보존한다.

ALTER TABLE daily_market_report
    ADD COLUMN market_condition ENUM('BULL', 'BEAR', 'NEUTRAL') NULL
        COMMENT '오늘의 AI 판단 시장 상황' AFTER content,
    ADD COLUMN report_status ENUM('NORMAL', 'PARTIAL', 'STALE') NULL
        COMMENT '리포트 전체 상태' AFTER market_condition,
    ADD COLUMN generation_source ENUM('OPENAI', 'FALLBACK') NULL
        COMMENT '서술 생성 출처' AFTER report_status,
    ADD COLUMN model_name VARCHAR(50) NULL
        COMMENT '생성에 사용한 모델명' AFTER generation_source,
    ADD COLUMN prompt_version VARCHAR(50) NULL
        COMMENT 'Structured Outputs 프롬프트 버전' AFTER model_name;

UPDATE daily_market_report
SET market_condition = 'NEUTRAL',
    report_status = 'NORMAL',
    generation_source = 'FALLBACK',
    model_name = 'legacy',
    prompt_version = 'legacy'
WHERE market_condition IS NULL
   OR report_status IS NULL
   OR generation_source IS NULL
   OR model_name IS NULL
   OR prompt_version IS NULL;

ALTER TABLE daily_market_report
    MODIFY COLUMN content TEXT NOT NULL
        COMMENT 'OpenAI GPT(gpt-5-nano)가 생성한 오늘의 시장 경향 리포트 텍스트',
    MODIFY COLUMN market_condition ENUM('BULL', 'BEAR', 'NEUTRAL') NOT NULL
        COMMENT '오늘의 AI 판단 시장 상황',
    MODIFY COLUMN report_status ENUM('NORMAL', 'PARTIAL', 'STALE') NOT NULL DEFAULT 'NORMAL'
        COMMENT '리포트 전체 상태',
    MODIFY COLUMN generation_source ENUM('OPENAI', 'FALLBACK') NOT NULL
        COMMENT '서술 생성 출처',
    MODIFY COLUMN model_name VARCHAR(50) NOT NULL
        COMMENT '생성에 사용한 모델명(gpt-5-nano)',
    MODIFY COLUMN prompt_version VARCHAR(50) NOT NULL
        COMMENT 'Structured Outputs 프롬프트 버전';

CREATE TABLE daily_market_indicator (
    indicator_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_id BIGINT NOT NULL,
    indicator_type ENUM('KOSPI', 'KOSDAQ', 'US_TREASURY_10Y', 'USD_KRW') NOT NULL,
    data_as_of TIMESTAMP NULL,
    source VARCHAR(100) NOT NULL,
    observed_value DECIMAL(18,4) NULL,
    change_value DECIMAL(18,4) NULL,
    change_rate DECIMAL(6,2) NULL,
    status ENUM('NORMAL', 'DELAYED', 'MISSING') NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_daily_market_indicator UNIQUE (report_id, indicator_type),
    CONSTRAINT fk_daily_market_indicator_report
        FOREIGN KEY (report_id) REFERENCES daily_market_report(report_id)
            ON DELETE CASCADE
) COMMENT='오늘의 AI투자리포트 지표별 원본값 — 리포트 1건당 4행'
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;
