-- 回退授权审计游标索引；仅删除索引，不影响审计数据。重复执行会检查并跳过。
SET @iam_audit_actor_index_sql = IF(
    EXISTS(SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'iam_authorization_audit'
          AND index_name = 'idx_iam_audit_tenant_actor_time'),
    'ALTER TABLE iam_authorization_audit DROP INDEX idx_iam_audit_tenant_actor_time',
    'SELECT 1');
PREPARE iam_audit_actor_index_statement FROM @iam_audit_actor_index_sql;
EXECUTE iam_audit_actor_index_statement;
DEALLOCATE PREPARE iam_audit_actor_index_statement;

SET @iam_audit_target_index_sql = IF(
    EXISTS(SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'iam_authorization_audit'
          AND index_name = 'idx_iam_audit_tenant_target_time'),
    'ALTER TABLE iam_authorization_audit DROP INDEX idx_iam_audit_tenant_target_time',
    'SELECT 1');
PREPARE iam_audit_target_index_statement FROM @iam_audit_target_index_sql;
EXECUTE iam_audit_target_index_statement;
DEALLOCATE PREPARE iam_audit_target_index_statement;
