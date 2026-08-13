-- 일정 등록 화면의 이벤트명과 휴가모드 자동 전환 여부를 저장한다.
-- 컬럼·제약·인덱스 존재 여부를 확인하므로 이미 수동 반영한 DB에도 재실행할 수 있다.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET time_zone = '+09:00';

-- MySQL DDL은 자동 커밋되므로 데이터와 기존 객체 정의를 첫 ALTER 전에 검증합니다.
CREATE TEMPORARY TABLE leave_mode_migration_preflight (
    validation_name VARCHAR(100) NOT NULL,
    invalid_count   INT NOT NULL,

    CONSTRAINT chk_leave_mode_migration_preflight
        CHECK (invalid_count = 0)
);

INSERT INTO leave_mode_migration_preflight (validation_name, invalid_count)
SELECT
    '기간 또는 예산 데이터',
    COUNT(*)
FROM leave_mode
WHERE start_date > end_date
   OR budget_amount < 0
UNION ALL
SELECT
    'chk_leave_mode_period 정의',
    COUNT(*)
FROM information_schema.CHECK_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND CONSTRAINT_NAME = 'chk_leave_mode_period'
  AND LOWER(
      REPLACE(REPLACE(REPLACE(REPLACE(CHECK_CLAUSE, ' ', ''), '`', ''), '(', ''), ')', '')
  ) <> 'start_date<=end_date'
UNION ALL
SELECT
    'chk_leave_mode_budget 정의',
    COUNT(*)
FROM information_schema.CHECK_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND CONSTRAINT_NAME = 'chk_leave_mode_budget'
  AND LOWER(
      REPLACE(REPLACE(REPLACE(REPLACE(CHECK_CLAUSE, ' ', ''), '`', ''), '(', ''), ')', '')
  ) <> 'budget_amountisnullorbudget_amount>=0'
UNION ALL
SELECT
    'idx_leave_mode_user_enabled_period 정의',
    COUNT(*)
FROM (
    SELECT CONCAT(
        MIN(NON_UNIQUE),
        ':',
        GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',')
    ) AS index_definition
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'leave_mode'
      AND INDEX_NAME = 'idx_leave_mode_user_enabled_period'
    GROUP BY INDEX_NAME
) existing_index
WHERE existing_index.index_definition
    <> '1:user_id,is_leave_mode_enabled,start_date,end_date';

DROP TEMPORARY TABLE leave_mode_migration_preflight;

SET @migration_sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'leave_mode'
          AND COLUMN_NAME = 'event_name'
    ),
    'DO 0',
    'ALTER TABLE leave_mode ADD COLUMN event_name VARCHAR(100) NULL COMMENT ''이벤트명(휴가, 외출, 외박 등)'' AFTER user_id'
);
PREPARE migration_stmt FROM @migration_sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

UPDATE leave_mode
SET event_name = '휴가 일정'
WHERE event_name IS NULL
   OR TRIM(event_name) = '';

ALTER TABLE leave_mode
    MODIFY COLUMN event_name VARCHAR(100) NOT NULL
        COMMENT '이벤트명(휴가, 외출, 외박 등)';

SET @migration_sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'leave_mode'
          AND COLUMN_NAME = 'is_leave_mode_enabled'
    ),
    'DO 0',
    'ALTER TABLE leave_mode ADD COLUMN is_leave_mode_enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT ''일정 기간 휴가모드 자동 전환 여부'' AFTER end_date'
);
PREPARE migration_stmt FROM @migration_sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

UPDATE leave_mode
SET is_leave_mode_enabled = TRUE
WHERE is_leave_mode_enabled IS NULL;

ALTER TABLE leave_mode
    MODIFY COLUMN is_leave_mode_enabled BOOLEAN NOT NULL DEFAULT TRUE
        COMMENT '일정 기간 휴가모드 자동 전환 여부';

SET @migration_sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'leave_mode'
          AND COLUMN_NAME = 'updated_at'
    ),
    'DO 0',
    'ALTER TABLE leave_mode ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''수정 일시'' AFTER created_at'
);
PREPARE migration_stmt FROM @migration_sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

ALTER TABLE leave_mode
    MODIFY COLUMN updated_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
        COMMENT '수정 일시';

SET @migration_sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
          AND TABLE_NAME = 'leave_mode'
          AND CONSTRAINT_NAME = 'chk_leave_mode_period'
    ),
    'DO 0',
    'ALTER TABLE leave_mode ADD CONSTRAINT chk_leave_mode_period CHECK (start_date <= end_date)'
);
PREPARE migration_stmt FROM @migration_sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @migration_sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
          AND TABLE_NAME = 'leave_mode'
          AND CONSTRAINT_NAME = 'chk_leave_mode_budget'
    ),
    'DO 0',
    'ALTER TABLE leave_mode ADD CONSTRAINT chk_leave_mode_budget CHECK (budget_amount IS NULL OR budget_amount >= 0)'
);
PREPARE migration_stmt FROM @migration_sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @migration_sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'leave_mode'
          AND INDEX_NAME = 'idx_leave_mode_user_enabled_period'
    ),
    'DO 0',
    'ALTER TABLE leave_mode ADD INDEX idx_leave_mode_user_enabled_period (user_id, is_leave_mode_enabled, start_date, end_date)'
);
PREPARE migration_stmt FROM @migration_sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

ALTER TABLE leave_mode
    COMMENT = '사용자 일정 및 휴가모드 자동 전환';
