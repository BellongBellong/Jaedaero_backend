-- 20260811_replace_ai_investment_report_with_gemini_market_report.sql의 과거 적용본에서
-- 아직 추가하지 않은 title/summary 필드를 기존 DB에 1회 추가하는 후속 migration이다.
-- 파일명이 원본 migration보다 뒤에 정렬되므로 적용 순서가 명확하다.
-- 신규 설치는 jaedaero_db_v1.sql의 최종 정의를 사용하며 이 파일은 적용하지 않는다.
-- 이 파일은 반복 실행 스크립트가 아니다.

ALTER TABLE daily_market_report
    ADD COLUMN title VARCHAR(200) NULL AFTER report_date,
    ADD COLUMN summary VARCHAR(500) NULL AFTER title;

UPDATE daily_market_report
SET title = '오늘의 AI 시장 리포트'
WHERE title IS NULL OR TRIM(title) = '';

UPDATE daily_market_report
SET summary = '기존 리포트는 새 구조의 사실 기반 본문으로 재생성되기 전까지 안전한 대체 상태로 제공됩니다.'
WHERE summary IS NULL OR TRIM(summary) = '';

ALTER TABLE daily_market_report
    MODIFY COLUMN title VARCHAR(200) NOT NULL
        COMMENT '오늘의 AI 시장 리포트 제목',
    MODIFY COLUMN summary VARCHAR(500) NOT NULL
        COMMENT '오늘의 AI 시장 리포트 한줄 요약';
