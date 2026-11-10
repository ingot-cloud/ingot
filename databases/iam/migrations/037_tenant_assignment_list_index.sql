-- 已有库补齐租户分配列表覆盖索引。新建库的 003 已包含该索引。
SET @iam_assignment_list_index_sql = IF(
    EXISTS(SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'iam_role_assignment'
          AND index_name = 'idx_iam_assignment_tenant_list'),
    'SELECT 1',
    'ALTER TABLE iam_role_assignment ADD INDEX idx_iam_assignment_tenant_list (tenant_id, domain, id, role_id, subject_type, delegation_grant_id, tenant_member_id, tenant_group_id), ALGORITHM=INPLACE, LOCK=NONE');
PREPARE iam_assignment_list_index_statement FROM @iam_assignment_list_index_sql;
EXECUTE iam_assignment_list_index_statement;
DEALLOCATE PREPARE iam_assignment_list_index_statement;
