-- 删除租户分配列表覆盖索引，保留分配数据。
SET @iam_assignment_list_index_sql = IF(
    EXISTS(SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'iam_role_assignment'
          AND index_name = 'idx_iam_assignment_tenant_list'),
    'ALTER TABLE iam_role_assignment DROP INDEX idx_iam_assignment_tenant_list, ALGORITHM=INPLACE, LOCK=NONE',
    'SELECT 1');
PREPARE iam_assignment_list_index_statement FROM @iam_assignment_list_index_sql;
EXECUTE iam_assignment_list_index_statement;
DEALLOCATE PREPARE iam_assignment_list_index_statement;
