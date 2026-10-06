-- 已有目标库的增量索引；全新 004 已含同名索引。重复执行不覆盖数据。
SET @iam_assignment_index_sql = IF(
    EXISTS(SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'iam_authorization_audit'
          AND index_name = 'idx_iam_audit_assignment_create'),
    'SELECT 1',
    'ALTER TABLE iam_authorization_audit ADD INDEX idx_iam_audit_assignment_create (assignment_id, change_type, occurred_at, id)');
PREPARE iam_assignment_index_statement FROM @iam_assignment_index_sql;
EXECUTE iam_assignment_index_statement;
DEALLOCATE PREPARE iam_assignment_index_statement;
