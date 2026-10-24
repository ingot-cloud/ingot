-- 已有库补齐授权审计按操作者、按对象游标翻页的索引；新建库004已含同名索引。重复执行会检查并跳过。
SET @iam_audit_actor_index_sql = IF(
    EXISTS(SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'iam_authorization_audit'
          AND index_name = 'idx_iam_audit_tenant_actor_time'),
    'SELECT 1',
    'ALTER TABLE iam_authorization_audit ADD INDEX idx_iam_audit_tenant_actor_time (tenant_id, actor_member_id, occurred_at, id), ALGORITHM=INPLACE, LOCK=NONE');
PREPARE iam_audit_actor_index_statement FROM @iam_audit_actor_index_sql;
EXECUTE iam_audit_actor_index_statement;
DEALLOCATE PREPARE iam_audit_actor_index_statement;

SET @iam_audit_target_index_sql = IF(
    EXISTS(SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'iam_authorization_audit'
          AND index_name = 'idx_iam_audit_tenant_target_time'),
    'SELECT 1',
    'ALTER TABLE iam_authorization_audit ADD INDEX idx_iam_audit_tenant_target_time (tenant_id, target_type, target_id, occurred_at, id), ALGORITHM=INPLACE, LOCK=NONE');
PREPARE iam_audit_target_index_statement FROM @iam_audit_target_index_sql;
EXECUTE iam_audit_target_index_statement;
DEALLOCATE PREPARE iam_audit_target_index_statement;
