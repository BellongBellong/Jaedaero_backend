-- 오늘의 AI 시장 리포트 노출 구간을 배치 시작 시각과 동일한 17:00로 앞당긴다.
-- 기존 18:00~익일 17:59:59 계약으로 저장된 행만 변경해 신규 계약 행의 중복 보정을 방지한다.

UPDATE daily_market_report
SET valid_from = DATE_SUB(valid_from, INTERVAL 1 HOUR),
    valid_until = DATE_SUB(valid_until, INTERVAL 1 HOUR)
WHERE TIME(valid_from) = '18:00:00'
  AND TIME(valid_until) = '17:59:59';

ALTER TABLE daily_market_report
    MODIFY COLUMN report_date DATE NOT NULL
        COMMENT '서비스 기준일(17:00~익일 16:59:59 노출 구간의 기준 날짜)',
    MODIFY COLUMN valid_from TIMESTAMP NOT NULL
        COMMENT '노출 시작 시각(해당일 17:00)',
    MODIFY COLUMN valid_until TIMESTAMP NOT NULL
        COMMENT '노출 종료 시각(익일 16:59:59)';
