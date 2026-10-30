-- 已有库把字段策略从单一成员场景改为按资源接入。
-- 新库使用 002/004/006，不执行本补丁。可重复执行：缺列才增加，已回填的成员资源不改写规则内容。
SET time_zone = '+00:00';

SET @iam_add_resource_field_policy = IF(
    EXISTS(
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'iam_resource' AND COLUMN_NAME = 'field_policy'),
    'SELECT 1',
    'ALTER TABLE iam_resource ADD COLUMN field_policy BOOLEAN NOT NULL DEFAULT FALSE COMMENT ''是否接入租户字段策略：1接入、0仅使用角色字段权限'' AFTER field_capabilities');
PREPARE iam_add_resource_field_policy FROM @iam_add_resource_field_policy;
EXECUTE iam_add_resource_field_policy;
DEALLOCATE PREPARE iam_add_resource_field_policy;

SET @iam_add_resource_field_policy_check = IF(
    EXISTS(
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'iam_resource' AND CONSTRAINT_NAME = 'ck_iam_resource_field_policy'),
    'SELECT 1',
    'ALTER TABLE iam_resource ADD CONSTRAINT ck_iam_resource_field_policy CHECK (field_policy IN (0, 1))');
PREPARE iam_add_resource_field_policy_check FROM @iam_add_resource_field_policy_check;
EXECUTE iam_add_resource_field_policy_check;
DEALLOCATE PREPARE iam_add_resource_field_policy_check;

UPDATE iam_resource resource
    JOIN iam_application app ON app.id = resource.application_id
SET resource.field_policy = TRUE
WHERE app.domain = 'TENANT' AND app.code = 'iam-tenant' AND resource.code = 'member';

UPDATE iam_resource resource
    JOIN iam_application app ON app.id = resource.application_id
SET resource.name = '字段策略'
WHERE app.code = 'iam-tenant' AND resource.code = 'field-policy' AND resource.name = '成员字段策略';

UPDATE iam_action SET name = '查看字段策略' WHERE code = 'iam-tenant:field-policy:read' AND name = '查看成员字段策略';
UPDATE iam_action SET name = '编辑字段策略' WHERE code = 'iam-tenant:field-policy:update' AND name = '编辑成员字段策略';

UPDATE iam_menu menu
    JOIN iam_application app ON app.id = menu.application_id
SET menu.name = '字段策略',
    menu.route_name = 'org.iam.field.policies',
    menu.view_path = 'org.iam.field.policies',
    menu.path = '/org/field-policies'
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.member.policies';

SET @iam_add_revision_resource = IF(
    EXISTS(
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'iam_default_policy_revision' AND COLUMN_NAME = 'resource_id'),
    'SELECT 1',
    'ALTER TABLE iam_default_policy_revision ADD COLUMN resource_id BIGINT UNSIGNED NULL COMMENT ''所属资源ID，该修订只作为这一资源的默认基线'' AFTER id');
PREPARE iam_add_revision_resource FROM @iam_add_revision_resource;
EXECUTE iam_add_revision_resource;
DEALLOCATE PREPARE iam_add_revision_resource;

UPDATE iam_default_policy_revision revision
    JOIN iam_resource resource ON resource.code = 'member'
    JOIN iam_application app ON app.id = resource.application_id AND app.domain = 'TENANT' AND app.code = 'iam-tenant'
SET revision.resource_id = resource.id
WHERE revision.kind = 'FIELD' AND revision.resource_id IS NULL;

SET @iam_revision_resource_not_null = IF(
    EXISTS(
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'iam_default_policy_revision'
          AND COLUMN_NAME = 'resource_id' AND IS_NULLABLE = 'YES'),
    'ALTER TABLE iam_default_policy_revision MODIFY COLUMN resource_id BIGINT UNSIGNED NOT NULL COMMENT ''所属资源ID，该修订只作为这一资源的默认基线''',
    'SELECT 1');
PREPARE iam_revision_resource_not_null FROM @iam_revision_resource_not_null;
EXECUTE iam_revision_resource_not_null;
DEALLOCATE PREPARE iam_revision_resource_not_null;

SET @iam_drop_revision_unique = IF(
    EXISTS(
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'iam_default_policy_revision'
          AND INDEX_NAME = 'uk_iam_default_policy_revision' AND COLUMN_NAME = 'kind' AND SEQ_IN_INDEX = 1),
    'ALTER TABLE iam_default_policy_revision DROP INDEX uk_iam_default_policy_revision',
    'SELECT 1');
PREPARE iam_drop_revision_unique FROM @iam_drop_revision_unique;
EXECUTE iam_drop_revision_unique;
DEALLOCATE PREPARE iam_drop_revision_unique;

SET @iam_add_revision_unique = IF(
    EXISTS(
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'iam_default_policy_revision'
          AND INDEX_NAME = 'uk_iam_default_policy_revision'),
    'SELECT 1',
    'ALTER TABLE iam_default_policy_revision ADD UNIQUE KEY uk_iam_default_policy_revision (resource_id, kind, revision)');
PREPARE iam_add_revision_unique FROM @iam_add_revision_unique;
EXECUTE iam_add_revision_unique;
DEALLOCATE PREPARE iam_add_revision_unique;

SET @iam_add_revision_resource_key = IF(
    EXISTS(
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'iam_default_policy_revision'
          AND INDEX_NAME = 'uk_iam_default_policy_resource'),
    'SELECT 1',
    'ALTER TABLE iam_default_policy_revision ADD UNIQUE KEY uk_iam_default_policy_resource (id, resource_id)');
PREPARE iam_add_revision_resource_key FROM @iam_add_revision_resource_key;
EXECUTE iam_add_revision_resource_key;
DEALLOCATE PREPARE iam_add_revision_resource_key;

SET @iam_add_revision_resource_fk = IF(
    EXISTS(
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'iam_default_policy_revision'
          AND CONSTRAINT_NAME = 'fk_iam_default_policy_resource'),
    'SELECT 1',
    'ALTER TABLE iam_default_policy_revision ADD CONSTRAINT fk_iam_default_policy_resource FOREIGN KEY (resource_id) REFERENCES iam_resource (id)');
PREPARE iam_add_revision_resource_fk FROM @iam_add_revision_resource_fk;
EXECUTE iam_add_revision_resource_fk;
DEALLOCATE PREPARE iam_add_revision_resource_fk;

SET @iam_add_policy_resource = IF(
    EXISTS(
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'iam_field_policy' AND COLUMN_NAME = 'resource_id'),
    'SELECT 1',
    'ALTER TABLE iam_field_policy ADD COLUMN resource_id BIGINT UNSIGNED NULL COMMENT ''接入字段策略的资源ID'' AFTER tenant_id');
PREPARE iam_add_policy_resource FROM @iam_add_policy_resource;
EXECUTE iam_add_policy_resource;
DEALLOCATE PREPARE iam_add_policy_resource;

UPDATE iam_field_policy policy
    JOIN iam_resource resource ON resource.code = 'member'
    JOIN iam_application app ON app.id = resource.application_id AND app.domain = 'TENANT' AND app.code = 'iam-tenant'
SET policy.resource_id = resource.id
WHERE policy.resource_id IS NULL;

SET @iam_drop_rule_policy_fk = IF(
    EXISTS(
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'iam_field_rule'
          AND CONSTRAINT_NAME = 'fk_iam_field_rule_policy' AND CONSTRAINT_TYPE = 'FOREIGN KEY'),
    'ALTER TABLE iam_field_rule DROP FOREIGN KEY fk_iam_field_rule_policy',
    'SELECT 1');
PREPARE iam_drop_rule_policy_fk FROM @iam_drop_rule_policy_fk;
EXECUTE iam_drop_rule_policy_fk;
DEALLOCATE PREPARE iam_drop_rule_policy_fk;

SET @iam_policy_pk_tenant = IF(
    EXISTS(
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'iam_field_policy'
          AND INDEX_NAME = 'PRIMARY' AND COLUMN_NAME = 'tenant_id' AND SEQ_IN_INDEX = 1)
    AND NOT EXISTS(
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'iam_field_policy'
          AND INDEX_NAME = 'PRIMARY' AND COLUMN_NAME = 'resource_id'),
    'ALTER TABLE iam_field_policy DROP PRIMARY KEY, ADD PRIMARY KEY (tenant_id, resource_id)',
    'SELECT 1');
PREPARE iam_policy_pk_tenant FROM @iam_policy_pk_tenant;
EXECUTE iam_policy_pk_tenant;
DEALLOCATE PREPARE iam_policy_pk_tenant;

SET @iam_policy_resource_not_null = IF(
    EXISTS(
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'iam_field_policy'
          AND COLUMN_NAME = 'resource_id' AND IS_NULLABLE = 'YES'),
    'ALTER TABLE iam_field_policy MODIFY COLUMN resource_id BIGINT UNSIGNED NOT NULL COMMENT ''接入字段策略的资源ID''',
    'SELECT 1');
PREPARE iam_policy_resource_not_null FROM @iam_policy_resource_not_null;
EXECUTE iam_policy_resource_not_null;
DEALLOCATE PREPARE iam_policy_resource_not_null;

SET @iam_add_policy_resource_fk = IF(
    EXISTS(
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'iam_field_policy'
          AND CONSTRAINT_NAME = 'fk_iam_field_policy_resource'),
    'SELECT 1',
    'ALTER TABLE iam_field_policy ADD CONSTRAINT fk_iam_field_policy_resource FOREIGN KEY (resource_id) REFERENCES iam_resource (id)');
PREPARE iam_add_policy_resource_fk FROM @iam_add_policy_resource_fk;
EXECUTE iam_add_policy_resource_fk;
DEALLOCATE PREPARE iam_add_policy_resource_fk;

SET @iam_add_policy_revision_fk = IF(
    EXISTS(
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'iam_field_policy'
          AND CONSTRAINT_NAME = 'fk_iam_field_policy_revision'),
    'SELECT 1',
    'ALTER TABLE iam_field_policy ADD CONSTRAINT fk_iam_field_policy_revision FOREIGN KEY (default_revision_id, resource_id) REFERENCES iam_default_policy_revision (id, resource_id)');
PREPARE iam_add_policy_revision_fk FROM @iam_add_policy_revision_fk;
EXECUTE iam_add_policy_revision_fk;
DEALLOCATE PREPARE iam_add_policy_revision_fk;

SET @iam_add_rule_resource = IF(
    EXISTS(
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'iam_field_rule' AND COLUMN_NAME = 'resource_id'),
    'SELECT 1',
    'ALTER TABLE iam_field_rule ADD COLUMN resource_id BIGINT UNSIGNED NULL COMMENT ''规则所属资源ID，与策略行一致'' AFTER tenant_id');
PREPARE iam_add_rule_resource FROM @iam_add_rule_resource;
EXECUTE iam_add_rule_resource;
DEALLOCATE PREPARE iam_add_rule_resource;

UPDATE iam_field_rule rule
    JOIN iam_field_policy policy ON policy.tenant_id = rule.tenant_id
SET rule.resource_id = policy.resource_id
WHERE rule.resource_id IS NULL;

SET @iam_rule_resource_not_null = IF(
    EXISTS(
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'iam_field_rule'
          AND COLUMN_NAME = 'resource_id' AND IS_NULLABLE = 'YES'),
    'ALTER TABLE iam_field_rule MODIFY COLUMN resource_id BIGINT UNSIGNED NOT NULL COMMENT ''规则所属资源ID，与策略行一致''',
    'SELECT 1');
PREPARE iam_rule_resource_not_null FROM @iam_rule_resource_not_null;
EXECUTE iam_rule_resource_not_null;
DEALLOCATE PREPARE iam_rule_resource_not_null;

SET @iam_drop_rule_scenario_check = IF(
    EXISTS(
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'iam_field_rule'
          AND CONSTRAINT_NAME = 'ck_iam_field_rule_scenario'),
    'ALTER TABLE iam_field_rule DROP CHECK ck_iam_field_rule_scenario',
    'SELECT 1');
PREPARE iam_drop_rule_scenario_check FROM @iam_drop_rule_scenario_check;
EXECUTE iam_drop_rule_scenario_check;
DEALLOCATE PREPARE iam_drop_rule_scenario_check;

SET @iam_drop_rule_scenario = IF(
    EXISTS(
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'iam_field_rule' AND COLUMN_NAME = 'scenario'),
    'ALTER TABLE iam_field_rule DROP COLUMN scenario',
    'SELECT 1');
PREPARE iam_drop_rule_scenario FROM @iam_drop_rule_scenario;
EXECUTE iam_drop_rule_scenario;
DEALLOCATE PREPARE iam_drop_rule_scenario;

SET @iam_drop_rule_match = IF(
    EXISTS(
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'iam_field_rule' AND INDEX_NAME = 'idx_iam_field_rule_match'),
    'ALTER TABLE iam_field_rule DROP INDEX idx_iam_field_rule_match',
    'SELECT 1');
PREPARE iam_drop_rule_match FROM @iam_drop_rule_match;
EXECUTE iam_drop_rule_match;
DEALLOCATE PREPARE iam_drop_rule_match;

SET @iam_add_rule_match = IF(
    EXISTS(
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'iam_field_rule' AND INDEX_NAME = 'idx_iam_field_rule_match'),
    'SELECT 1',
    'ALTER TABLE iam_field_rule ADD KEY idx_iam_field_rule_match (tenant_id, resource_id, field_key, id)');
PREPARE iam_add_rule_match FROM @iam_add_rule_match;
EXECUTE iam_add_rule_match;
DEALLOCATE PREPARE iam_add_rule_match;

SET @iam_add_rule_policy_fk = IF(
    EXISTS(
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'iam_field_rule'
          AND CONSTRAINT_NAME = 'fk_iam_field_rule_policy'),
    'SELECT 1',
    'ALTER TABLE iam_field_rule ADD CONSTRAINT fk_iam_field_rule_policy FOREIGN KEY (tenant_id, resource_id) REFERENCES iam_field_policy (tenant_id, resource_id)');
PREPARE iam_add_rule_policy_fk FROM @iam_add_rule_policy_fk;
EXECUTE iam_add_rule_policy_fk;
DEALLOCATE PREPARE iam_add_rule_policy_fk;

UPDATE iam_field_policy AS policy
    JOIN (
        SELECT source.tenant_id, source.resource_id,
               COALESCE((
                   SELECT JSON_ARRAYAGG(JSON_OBJECT(
                       'actionCode', extracted.action_code,
                       'fieldKey', extracted.field_key,
                       'viewerSelection', extracted.viewer_selection,
                       'operations', extracted.operations))
                   FROM JSON_TABLE(source.operation_rules, '$[*]' COLUMNS (
                       action_code VARCHAR(192) PATH '$.actionCode',
                       field_key VARCHAR(128) PATH '$.fieldKey',
                       viewer_selection JSON PATH '$.viewerSelection',
                       operations JSON PATH '$.operations'
                   )) AS extracted
               ), JSON_ARRAY()) AS operation_rules
        FROM iam_field_policy AS source
    ) AS rebuilt ON rebuilt.tenant_id = policy.tenant_id AND rebuilt.resource_id = policy.resource_id
SET policy.operation_rules = rebuilt.operation_rules
WHERE JSON_SEARCH(policy.operation_rules, 'one', 'MANAGEMENT', NULL, '$[*].scenario') IS NOT NULL
   OR JSON_CONTAINS_PATH(policy.operation_rules, 'one', '$[*].resource');
