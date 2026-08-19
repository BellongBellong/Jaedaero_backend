-- 휴가모드 일정 목록과 삭제 처리를 위해 논리 삭제 일시와 조회 인덱스를 추가한다.
SET @migration_sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'leave_mode'
          AND COLUMN_NAME = 'deleted_at'
    ),
    'DO 0',
    'ALTER TABLE leave_mode ADD COLUMN deleted_at TIMESTAMP NULL COMMENT ''삭제 일시'' AFTER is_leave_mode_enabled'
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
          AND INDEX_NAME = 'idx_leave_mode_user_deleted_start_date'
    ),
    'DO 0',
    'ALTER TABLE leave_mode ADD INDEX idx_leave_mode_user_deleted_start_date (user_id, deleted_at, start_date)'
);
PREPARE migration_stmt FROM @migration_sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;
