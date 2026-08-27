-- CODEF 적금 동기화 및 시연 적금의 출처를 구분합니다.
SET @source_type_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'soldier_saving'
      AND column_name = 'source_type'
);

SET @sql := IF(
    @source_type_exists = 0,
    'ALTER TABLE soldier_saving ADD COLUMN source_type ENUM(''CODEF'', ''DEMO'') NOT NULL DEFAULT ''CODEF'' COMMENT ''적금 데이터 출처'' AFTER account_id',
    'SELECT 1'
);
PREPARE statement FROM @sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;
