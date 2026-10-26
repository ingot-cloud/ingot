-- 已有库移除尚未上线的普通通讯录浏览、可见范围策略与策略预览。
-- 按应用编码、菜单路由和操作码定位，不依赖保留 ID。菜单文件夹 kind=DIRECTORY 保持不变。
-- 先删除目录策略表，再删除 kind=DIRECTORY 的默认版本：iam_directory_policy 外键引用 (id, kind)。
-- 角色字段快照写入时已跳过通讯录资源，本脚本不改写 resource_field_permissions JSON。
SET time_zone = '+00:00';

DELETE menu_action FROM iam_menu_action menu_action
    JOIN iam_menu menu ON menu.application_id = menu_action.application_id AND menu.id = menu_action.menu_id
    JOIN iam_application app ON app.id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.directory';

DELETE menu FROM iam_menu menu
    JOIN iam_application app ON app.id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.directory';

DELETE menu_action FROM iam_menu_action menu_action
    JOIN iam_menu menu ON menu.application_id = menu_action.application_id AND menu.id = menu_action.menu_id
    JOIN iam_application app ON app.id = menu.application_id
    JOIN iam_action action ON action.application_id = menu_action.application_id AND action.id = menu_action.action_id
WHERE app.code = 'iam-tenant'
  AND menu.route_name = 'org.iam.workspace'
  AND action.code = 'iam-tenant:directory:read';

DELETE menu_action FROM iam_menu_action menu_action
    JOIN iam_menu menu ON menu.application_id = menu_action.application_id AND menu.id = menu_action.menu_id
    JOIN iam_application app ON app.id = menu.application_id
    JOIN iam_action action ON action.application_id = menu_action.application_id AND action.id = menu_action.action_id
WHERE app.code = 'iam-tenant'
  AND menu.route_name IN ('org.iam.member.policies', 'org.iam.compliance')
  AND action.code = 'iam-tenant:directory-policy:read';

DELETE menu_action FROM iam_menu_action menu_action
    JOIN iam_action action ON action.application_id = menu_action.application_id AND action.id = menu_action.action_id
WHERE action.code IN (
    'iam-tenant:directory:read',
    'iam-tenant:directory-policy:read',
    'iam-tenant:directory-policy:update',
    'iam-tenant:policy:preview');

DELETE role_grant FROM iam_role_grant role_grant
    JOIN iam_action action ON action.id = role_grant.action_id
WHERE action.code IN (
    'iam-tenant:directory:read',
    'iam-tenant:directory-policy:read',
    'iam-tenant:directory-policy:update',
    'iam-tenant:policy:preview');

DELETE role_delta FROM iam_role_delta role_delta
    JOIN iam_action action ON action.id = role_delta.action_id
WHERE action.code IN (
    'iam-tenant:directory:read',
    'iam-tenant:directory-policy:read',
    'iam-tenant:directory-policy:update',
    'iam-tenant:policy:preview');

DELETE ceiling FROM iam_delegation_action_ceiling ceiling
    JOIN iam_action action ON action.id = ceiling.action_id
WHERE action.code IN (
    'iam-tenant:directory:read',
    'iam-tenant:directory-policy:read',
    'iam-tenant:directory-policy:update',
    'iam-tenant:policy:preview');

DELETE parameter_row FROM iam_role_parameter parameter_row
    JOIN iam_resource resource ON resource.id = parameter_row.resource_id
    JOIN iam_application app ON app.id = resource.application_id
WHERE app.code = 'iam-tenant' AND resource.code IN ('directory', 'directory-policy', 'policy');

DELETE ceiling FROM iam_delegation_resource_ceiling ceiling
    JOIN iam_resource resource ON resource.id = ceiling.resource_id
    JOIN iam_application app ON app.id = resource.application_id
WHERE app.code = 'iam-tenant' AND resource.code IN ('directory', 'directory-policy', 'policy');

DELETE FROM iam_action
WHERE code IN (
    'iam-tenant:directory:read',
    'iam-tenant:directory-policy:read',
    'iam-tenant:directory-policy:update',
    'iam-tenant:policy:preview');

DELETE resource FROM iam_resource resource
    JOIN iam_application app ON app.id = resource.application_id
WHERE app.code = 'iam-tenant'
  AND resource.code IN ('directory', 'directory-policy', 'policy')
  AND NOT EXISTS (SELECT 1 FROM iam_action action WHERE action.resource_id = resource.id);

DELETE FROM iam_field_rule WHERE scenario = 'DIRECTORY';

UPDATE iam_field_policy AS policy
    JOIN (
        SELECT source.tenant_id,
               COALESCE((
                   SELECT JSON_ARRAYAGG(extracted.rule_item)
                   FROM JSON_TABLE(source.operation_rules, '$[*]' COLUMNS (
                       rule_item JSON PATH '$',
                       scenario VARCHAR(32) PATH '$.scenario',
                       resource_code VARCHAR(128) PATH '$.resource.resourceCode'
                   )) AS extracted
                   WHERE IFNULL(extracted.scenario, '') <> 'DIRECTORY'
                     AND IFNULL(extracted.resource_code, '') <> 'directory'
               ), JSON_ARRAY()) AS operation_rules
        FROM iam_field_policy AS source
    ) AS rebuilt ON rebuilt.tenant_id = policy.tenant_id
SET policy.operation_rules = rebuilt.operation_rules
WHERE JSON_SEARCH(policy.operation_rules, 'one', 'DIRECTORY', NULL, '$[*].scenario') IS NOT NULL
   OR JSON_SEARCH(policy.operation_rules, 'one', 'directory', NULL, '$[*].resource.resourceCode') IS NOT NULL;

DROP TABLE IF EXISTS iam_directory_rule;
DROP TABLE IF EXISTS iam_directory_policy;

DELETE FROM iam_default_policy_revision WHERE kind = 'DIRECTORY';

SET @iam_drop_default_kind_check = IF(
    EXISTS(
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS table_constraint
        JOIN information_schema.CHECK_CONSTRAINTS check_constraint
          ON check_constraint.CONSTRAINT_SCHEMA = table_constraint.CONSTRAINT_SCHEMA
         AND check_constraint.CONSTRAINT_NAME = table_constraint.CONSTRAINT_NAME
        WHERE table_constraint.TABLE_SCHEMA = DATABASE()
          AND table_constraint.TABLE_NAME = 'iam_default_policy_revision'
          AND table_constraint.CONSTRAINT_NAME = 'ck_iam_default_policy_kind'
          AND check_constraint.CHECK_CLAUSE LIKE '%DIRECTORY%'),
    'ALTER TABLE iam_default_policy_revision DROP CHECK ck_iam_default_policy_kind',
    'SELECT 1');
PREPARE iam_drop_default_kind_check FROM @iam_drop_default_kind_check;
EXECUTE iam_drop_default_kind_check;
DEALLOCATE PREPARE iam_drop_default_kind_check;

SET @iam_add_default_kind_check = IF(
    EXISTS(
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'iam_default_policy_revision'
          AND CONSTRAINT_NAME = 'ck_iam_default_policy_kind'),
    'SELECT 1',
    'ALTER TABLE iam_default_policy_revision ADD CONSTRAINT ck_iam_default_policy_kind CHECK (kind IN (''FIELD''))');
PREPARE iam_add_default_kind_check FROM @iam_add_default_kind_check;
EXECUTE iam_add_default_kind_check;
DEALLOCATE PREPARE iam_add_default_kind_check;

SET @iam_drop_field_scenario_check = IF(
    EXISTS(
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS table_constraint
        JOIN information_schema.CHECK_CONSTRAINTS check_constraint
          ON check_constraint.CONSTRAINT_SCHEMA = table_constraint.CONSTRAINT_SCHEMA
         AND check_constraint.CONSTRAINT_NAME = table_constraint.CONSTRAINT_NAME
        WHERE table_constraint.TABLE_SCHEMA = DATABASE()
          AND table_constraint.TABLE_NAME = 'iam_field_rule'
          AND table_constraint.CONSTRAINT_NAME = 'ck_iam_field_rule_scenario'
          AND check_constraint.CHECK_CLAUSE LIKE '%DIRECTORY%'),
    'ALTER TABLE iam_field_rule DROP CHECK ck_iam_field_rule_scenario',
    'SELECT 1');
PREPARE iam_drop_field_scenario_check FROM @iam_drop_field_scenario_check;
EXECUTE iam_drop_field_scenario_check;
DEALLOCATE PREPARE iam_drop_field_scenario_check;

SET @iam_add_field_scenario_check = IF(
    EXISTS(
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'iam_field_rule'
          AND CONSTRAINT_NAME = 'ck_iam_field_rule_scenario'),
    'SELECT 1',
    'ALTER TABLE iam_field_rule ADD CONSTRAINT ck_iam_field_rule_scenario CHECK (scenario IN (''MANAGEMENT''))');
PREPARE iam_add_field_scenario_check FROM @iam_add_field_scenario_check;
EXECUTE iam_add_field_scenario_check;
DEALLOCATE PREPARE iam_add_field_scenario_check;

ALTER TABLE iam_default_policy_revision
    COMMENT = '默认字段策略固定版本',
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL COMMENT '默认字段策略固定版本记录标识',
    MODIFY COLUMN kind VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '默认策略类别：FIELD字段',
    MODIFY COLUMN definition JSON NOT NULL COMMENT '默认字段策略不可变定义JSON对象，由字段求值器解释';

ALTER TABLE iam_field_policy
    MODIFY COLUMN default_kind VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'FIELD' COMMENT '默认策略种类，固定为FIELD';

ALTER TABLE iam_field_rule
    MODIFY COLUMN scenario VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '字段策略使用场景：MANAGEMENT管理；取值MANAGEMENT';
