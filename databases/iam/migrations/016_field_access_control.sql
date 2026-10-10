-- IAM 字段访问控制：保留数据的契约升级，适用于本次变更前的 IAM 库。
-- MySQL 8.0.16+；需要 CREATE ROUTINE / ALTER / CREATE / DML 权限。
-- 先停止所有 IAM 节点及其他数据库写入，备份所选库，停机至少30秒让字段缓存到期。
-- mysql --database=<明确选定的IAM库> < databases/iam/migrations/016_field_access_control.sql
-- 不切换数据库，不删除账号/成员/角色/分配，不执行006，不添加操作授权。
-- DML事务失败会回滚；DDL自动提交，不能用ROLLBACK整体回退，整体回退使用备份。
-- 支持重跑：新JSON保持不变，操作规则不重复，旧列/约束已移除时跳过。
-- iam_field_access_016_legacy_rule 是迁移留档，不是业务策略表；不要在验收前删除。
-- 原目标受限的编辑不能扩大为全局编辑，保守关闭并在最后列出，需管理台重新配置。
SET time_zone = '+00:00';

DROP PROCEDURE IF EXISTS ingot_iam_migrate_field_access_016;
DELIMITER $$
CREATE PROCEDURE ingot_iam_migrate_field_access_016()
BEGIN
    DECLARE has_legacy_editable BOOLEAN DEFAULT FALSE;
    DECLARE changed_resources BIGINT DEFAULT 0;
    DECLARE changed_roles BIGINT DEFAULT 0;
    DECLARE changed_defaults BIGINT DEFAULT 0;
    DECLARE changed_operations BIGINT DEFAULT 0;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    IF DATABASE() IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Select the IAM database before migration 016';
    END IF;
    IF (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE()
            AND table_name IN ('iam_resource', 'iam_application', 'iam_role_revision',
                'iam_default_policy_revision', 'iam_field_policy', 'iam_field_rule')) <> 6 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Migration 016 requires the existing IAM schema';
    END IF;

    SELECT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
        AND table_name = 'iam_field_rule' AND column_name = 'editable') INTO has_legacy_editable;

    IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
            AND table_name = 'iam_field_policy' AND column_name = 'operation_rules') THEN
        ALTER TABLE iam_field_policy
            ADD COLUMN operation_rules JSON NOT NULL DEFAULT (JSON_ARRAY()) AFTER default_kind;
    END IF;
    IF NOT EXISTS(SELECT 1 FROM information_schema.table_constraints WHERE constraint_schema = DATABASE()
            AND table_name = 'iam_field_policy' AND constraint_name = 'ck_iam_field_operations') THEN
        ALTER TABLE iam_field_policy ADD CONSTRAINT ck_iam_field_operations
            CHECK (JSON_TYPE(operation_rules) = 'ARRAY');
    END IF;

    CREATE TABLE IF NOT EXISTS iam_field_access_016_legacy_rule (
        rule_id BIGINT UNSIGNED NOT NULL PRIMARY KEY,
        tenant_id BIGINT UNSIGNED NOT NULL,
        scenario VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
        field_key VARCHAR(128) COLLATE utf8mb4_bin NOT NULL,
        viewer_selection JSON NOT NULL,
        target_scope JSON NOT NULL,
        scope_bindings JSON NOT NULL,
        legacy_visibility VARCHAR(8) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
        legacy_editable BOOLEAN NOT NULL,
        migrated_editable BOOLEAN NOT NULL,
        requires_review BOOLEAN NOT NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
    DROP TEMPORARY TABLE IF EXISTS iam_field_access_016_changed_defaults;
    CREATE TEMPORARY TABLE iam_field_access_016_changed_defaults (id BIGINT UNSIGNED PRIMARY KEY);

    START TRANSACTION;

    -- 1. 所有资源：仅移除sortable；MASKED缺规则时按phone/email/其他文本补规则。
    BEGIN
        DECLARE finished BOOLEAN DEFAULT FALSE;
        DECLARE row_id BIGINT UNSIGNED;
        DECLARE app_code VARCHAR(64);
        DECLARE resource_code VARCHAR(64);
        DECLARE original JSON;
        DECLARE converted JSON;
        DECLARE item JSON;
        DECLARE seen JSON;
        DECLARE field_key VARCHAR(128);
        DECLARE position_index INT;
        DECLARE fields_cursor CURSOR FOR SELECT r.id, a.code, r.code, r.field_capabilities
            FROM iam_resource r JOIN iam_application a ON a.id = r.application_id;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET finished = TRUE;
        OPEN fields_cursor;
        fields_loop: LOOP
            FETCH fields_cursor INTO row_id, app_code, resource_code, original;
            IF finished THEN LEAVE fields_loop; END IF;
            SET converted = JSON_ARRAY(), seen = JSON_OBJECT(), position_index = 0;
            WHILE position_index < JSON_LENGTH(original) DO
                SET item = JSON_EXTRACT(original, CONCAT('$[', position_index, ']'));
                SET field_key = JSON_UNQUOTE(JSON_EXTRACT(item, '$.key'));
                IF field_key IS NULL OR field_key = '' OR JSON_TYPE(item) <> 'OBJECT'
                        OR COALESCE(JSON_TYPE(JSON_EXTRACT(item, '$.visibilities')), 'NULL') <> 'ARRAY'
                        OR JSON_LENGTH(JSON_EXTRACT(item, '$.visibilities')) = 0
                        OR COALESCE(JSON_TYPE(JSON_EXTRACT(item, '$.label')), 'NULL') <> 'STRING'
                        OR JSON_CONTAINS_PATH(seen, 'one', CONCAT('$.', JSON_QUOTE(field_key))) THEN
                    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid or duplicate resource field; inspect field_capabilities';
                END IF;
                IF EXISTS(SELECT 1 FROM JSON_TABLE(item, '$.visibilities[*]'
                        COLUMNS(value VARCHAR(32) PATH '$')) v WHERE v.value IS NULL OR v.value NOT IN ('HIDDEN', 'MASKED', 'FULL'))
                        OR (SELECT COUNT(*) - COUNT(DISTINCT v.value) FROM JSON_TABLE(item, '$.visibilities[*]'
                            COLUMNS(value VARCHAR(32) PATH '$')) v) <> 0 THEN
                    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid resource visibility list; migration aborted';
                END IF;
                SET seen = JSON_SET(seen, CONCAT('$.', JSON_QUOTE(field_key)), TRUE);
                SET item = JSON_REMOVE(item, '$.sortable');
                IF JSON_CONTAINS(JSON_EXTRACT(item, '$.visibilities'), JSON_QUOTE('MASKED')) THEN
                    IF JSON_EXTRACT(item, '$.mask') IS NULL OR JSON_TYPE(JSON_EXTRACT(item, '$.mask')) = 'NULL' THEN
                        SET item = JSON_SET(item, '$.mask', JSON_OBJECT('kind',
                            CASE field_key WHEN 'phone' THEN 'PHONE' WHEN 'email' THEN 'EMAIL' ELSE 'ALL' END));
                    END IF;
                ELSE
                    SET item = JSON_REMOVE(item, '$.mask');
                END IF;
                -- 平台成员仅有显示名筛选实现，避免继承旧手机/邮箱的无效筛选声明。
                IF app_code = 'iam-platform' AND resource_code = 'member' AND field_key <> 'displayName' THEN
                    SET item = JSON_SET(item, '$.filterable', JSON_EXTRACT('false', '$'));
                END IF;
                SET converted = JSON_ARRAY_APPEND(converted, '$', item);
                SET position_index = position_index + 1;
            END WHILE;
            -- 补能力目录，不给自定义角色追加时间字段可见性授权。
            IF app_code = 'iam-platform' AND resource_code = 'member' THEN
                IF NOT JSON_CONTAINS_PATH(seen, 'one', '$.joinedAt') THEN
                    SET converted = JSON_ARRAY_APPEND(converted, '$', JSON_OBJECT('key', 'joinedAt', 'label', '加入时间',
                        'visibilities', JSON_ARRAY('HIDDEN', 'FULL'), 'editable', JSON_EXTRACT('false', '$'), 'filterable', JSON_EXTRACT('false', '$')));
                END IF;
                IF NOT JSON_CONTAINS_PATH(seen, 'one', '$.lastLoginAt') THEN
                    SET converted = JSON_ARRAY_APPEND(converted, '$', JSON_OBJECT('key', 'lastLoginAt', 'label', '最后登录',
                        'visibilities', JSON_ARRAY('HIDDEN', 'FULL'), 'editable', JSON_EXTRACT('false', '$'), 'filterable', JSON_EXTRACT('false', '$')));
                END IF;
                IF NOT JSON_CONTAINS_PATH(seen, 'one', '$.updatedAt') THEN
                    SET converted = JSON_ARRAY_APPEND(converted, '$', JSON_OBJECT('key', 'updatedAt', 'label', '更新时间',
                        'visibilities', JSON_ARRAY('HIDDEN', 'FULL'), 'editable', JSON_EXTRACT('false', '$'), 'filterable', JSON_EXTRACT('false', '$')));
                END IF;
            END IF;
            IF NOT (original <=> converted) THEN
                UPDATE iam_resource SET field_capabilities = converted, version = version + 1 WHERE id = row_id;
                SET changed_resources = changed_resources + 1;
            END IF;
        END LOOP;
        CLOSE fields_cursor;
    END;

    -- 2. 所有固定角色版本（包括自定义/历史版本）：保留原ID、原visibility、原操作范围。
    BEGIN
        DECLARE finished BOOLEAN DEFAULT FALSE;
        DECLARE row_id BIGINT UNSIGNED;
        DECLARE original JSON;
        DECLARE converted JSON;
        DECLARE resource_keys JSON;
        DECLARE resource_key VARCHAR(128);
        DECLARE resource_path VARCHAR(300);
        DECLARE definition JSON;
        DECLARE field_keys JSON;
        DECLARE field_key VARCHAR(128);
        DECLARE field_path VARCHAR(300);
        DECLARE field_value JSON;
        DECLARE visibility_value VARCHAR(8);
        DECLARE visibility_map JSON;
        DECLARE operation_map JSON;
        DECLARE capability JSON;
        DECLARE resource_index INT;
        DECLARE field_index INT;
        DECLARE roles_cursor CURSOR FOR SELECT id, resource_field_permissions FROM iam_role_revision;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET finished = TRUE;
        OPEN roles_cursor;
        roles_loop: LOOP
            FETCH roles_cursor INTO row_id, original;
            IF finished THEN LEAVE roles_loop; END IF;
            SET converted = JSON_OBJECT(), resource_keys = JSON_KEYS(original), resource_index = 0;
            WHILE resource_index < JSON_LENGTH(resource_keys) DO
                SET resource_key = JSON_UNQUOTE(JSON_EXTRACT(resource_keys, CONCAT('$[', resource_index, ']')));
                SET resource_path = CONCAT('$.', JSON_QUOTE(resource_key));
                SET definition = JSON_EXTRACT(original, resource_path);
                IF JSON_TYPE(JSON_EXTRACT(definition, '$.visibility')) = 'OBJECT'
                        AND JSON_TYPE(JSON_EXTRACT(definition, '$.operations')) = 'OBJECT'
                        AND NOT EXISTS(SELECT 1 FROM JSON_TABLE(JSON_KEYS(JSON_EXTRACT(definition, '$.visibility')), '$[*]'
                            COLUMNS(key_name VARCHAR(128) PATH '$')) k
                            WHERE JSON_TYPE(JSON_EXTRACT(definition, CONCAT('$.visibility.', JSON_QUOTE(k.key_name)))) <> 'STRING')
                        AND NOT EXISTS(SELECT 1 FROM JSON_TABLE(JSON_KEYS(JSON_EXTRACT(definition, '$.operations')), '$[*]'
                            COLUMNS(key_name VARCHAR(128) PATH '$')) k
                            WHERE JSON_TYPE(JSON_EXTRACT(definition, CONCAT('$.operations.', JSON_QUOTE(k.key_name)))) <> 'OBJECT') THEN
                    SET converted = JSON_SET(converted, resource_path, definition);
                ELSE
                    SET field_keys = JSON_KEYS(definition), field_index = 0;
                    SET visibility_map = JSON_OBJECT(), operation_map = JSON_OBJECT();
                    IF JSON_TYPE(definition) <> 'OBJECT' THEN
                        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid role resource definition; inspect resource_field_permissions';
                    END IF;
                    WHILE field_index < JSON_LENGTH(field_keys) DO
                        SET field_key = JSON_UNQUOTE(JSON_EXTRACT(field_keys, CONCAT('$[', field_index, ']')));
                        SET field_path = CONCAT('$.', JSON_QUOTE(field_key));
                        SET field_value = JSON_EXTRACT(definition, field_path);
                        SET visibility_value = JSON_UNQUOTE(JSON_EXTRACT(field_value, '$.visibility'));
                        IF visibility_value IS NULL OR visibility_value NOT IN ('HIDDEN', 'MASKED', 'FULL') THEN
                            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid legacy role field visibility; migration aborted';
                        END IF;
                        -- 聚合空结果返回NULL，不影响游标结束标志；无目录能力的字段操作关闭。
                        SELECT MAX(CAST(j.field AS CHAR)) INTO capability
                        FROM iam_resource r JOIN JSON_TABLE(r.field_capabilities, '$[*]'
                            COLUMNS(field JSON PATH '$', field_name VARCHAR(128) PATH '$.key')) j
                        WHERE CAST(r.id AS CHAR) = resource_key AND j.field_name = field_key;
                        SET visibility_map = JSON_SET(visibility_map, field_path, visibility_value);
                        SET operation_map = JSON_SET(operation_map, field_path, JSON_OBJECT(
                            'editable', JSON_EXTRACT(IF(visibility_value = 'FULL'
                                AND JSON_UNQUOTE(JSON_EXTRACT(field_value, '$.editable')) IN ('true', '1')
                                AND JSON_UNQUOTE(JSON_EXTRACT(capability, '$.editable')) IN ('true', '1'), 'true', 'false'), '$'),
                            'filterable', JSON_EXTRACT(IF(visibility_value = 'FULL'
                                AND JSON_UNQUOTE(JSON_EXTRACT(capability, '$.filterable')) IN ('true', '1'), 'true', 'false'), '$')));
                        SET field_index = field_index + 1;
                    END WHILE;
                    SET converted = JSON_SET(converted, resource_path, JSON_OBJECT('visibility', visibility_map, 'operations', operation_map));
                END IF;
                SET resource_index = resource_index + 1;
            END WHILE;
            IF NOT (original <=> converted) THEN
                UPDATE iam_role_revision SET resource_field_permissions = converted WHERE id = row_id;
                SET changed_roles = changed_roles + 1;
            END IF;
        END LOOP;
        CLOSE roles_cursor;
    END;

    -- 3. 所有FIELD默认版本：fields/ceiling对象拆为可见性与operations/operationCeiling。
    BEGIN
        DECLARE finished BOOLEAN DEFAULT FALSE;
        DECLARE row_id BIGINT UNSIGNED;
        DECLARE original JSON;
        DECLARE converted JSON;
        DECLARE values_map JSON;
        DECLARE operation_map JSON;
        DECLARE field_keys JSON;
        DECLARE field_value JSON;
        DECLARE field_key VARCHAR(128);
        DECLARE field_path VARCHAR(300);
        DECLARE visibility_value VARCHAR(8);
        DECLARE section_name VARCHAR(32);
        DECLARE operation_name VARCHAR(32);
        DECLARE section_index INT;
        DECLARE field_index INT;
        DECLARE defaults_cursor CURSOR FOR SELECT id, definition FROM iam_default_policy_revision WHERE kind = 'FIELD';
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET finished = TRUE;
        OPEN defaults_cursor;
        defaults_loop: LOOP
            FETCH defaults_cursor INTO row_id, original;
            IF finished THEN LEAVE defaults_loop; END IF;
            SET converted = original, section_index = 0;
            WHILE section_index < 2 DO
                SET section_name = IF(section_index = 0, 'fields', 'ceiling');
                SET operation_name = IF(section_index = 0, 'operations', 'operationCeiling');
                SET values_map = JSON_EXTRACT(original, CONCAT('$.', section_name));
                SET operation_map = JSON_EXTRACT(original, CONCAT('$.', operation_name));
                IF operation_map IS NULL OR JSON_TYPE(operation_map) = 'NULL' THEN SET operation_map = JSON_OBJECT(); END IF;
                IF values_map IS NOT NULL AND JSON_TYPE(values_map) <> 'NULL' THEN
                    IF JSON_TYPE(values_map) <> 'OBJECT' THEN
                        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid FIELD default map; migration aborted';
                    END IF;
                    SET field_keys = JSON_KEYS(values_map), field_index = 0;
                    WHILE field_index < JSON_LENGTH(field_keys) DO
                        SET field_key = JSON_UNQUOTE(JSON_EXTRACT(field_keys, CONCAT('$[', field_index, ']')));
                        SET field_path = CONCAT('$.', JSON_QUOTE(field_key));
                        SET field_value = JSON_EXTRACT(values_map, field_path);
                        IF JSON_TYPE(field_value) = 'OBJECT' THEN
                            SET visibility_value = JSON_UNQUOTE(JSON_EXTRACT(field_value, '$.visibility'));
                            IF visibility_value IS NULL OR visibility_value NOT IN ('HIDDEN', 'MASKED', 'FULL') THEN
                                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid legacy FIELD default visibility; migration aborted';
                            END IF;
                            SET values_map = JSON_SET(values_map, field_path, visibility_value);
                            IF NOT JSON_CONTAINS_PATH(operation_map, 'one', field_path) THEN
                                SET operation_map = JSON_SET(operation_map, field_path, JSON_OBJECT(
                                    'editable', JSON_EXTRACT(IF(visibility_value = 'FULL'
                                        AND JSON_UNQUOTE(JSON_EXTRACT(field_value, '$.editable')) IN ('true', '1'), 'true', 'false'), '$'),
                                    'filterable', JSON_EXTRACT(IF(field_key IN ('phone', 'email'), 'true', 'false'), '$')));
                            END IF;
                        ELSEIF JSON_TYPE(field_value) <> 'STRING' OR JSON_UNQUOTE(field_value) NOT IN ('HIDDEN', 'MASKED', 'FULL') THEN
                            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid FIELD default visibility; migration aborted';
                        END IF;
                        SET field_index = field_index + 1;
                    END WHILE;
                    SET converted = JSON_SET(converted, CONCAT('$.', section_name), values_map);
                END IF;
                -- 旧定义缺键的原文档默认：显示名/头像可编辑，手机号/邮箱不可编辑。
                -- 旧筛选已经以整体FULL为门禁；保留手机/邮箱筛选，再由目录/绑定收紧。
                IF section_index = 0 AND NOT JSON_CONTAINS_PATH(original, 'one', '$.operations') THEN
                    SET field_index = 0, field_keys = JSON_ARRAY('displayName', 'avatar', 'phone', 'email');
                    WHILE field_index < 4 DO
                        SET field_key = JSON_UNQUOTE(JSON_EXTRACT(field_keys, CONCAT('$[', field_index, ']')));
                        SET field_path = CONCAT('$.', JSON_QUOTE(field_key));
                        IF NOT JSON_CONTAINS_PATH(operation_map, 'one', field_path) THEN
                            SET operation_map = JSON_SET(operation_map, field_path, JSON_OBJECT(
                                'editable', JSON_EXTRACT(IF(field_key IN ('displayName', 'avatar'), 'true', 'false'), '$'),
                                'filterable', JSON_EXTRACT(IF(field_key IN ('phone', 'email'), 'true', 'false'), '$')));
                        END IF;
                        SET field_index = field_index + 1;
                    END WHILE;
                END IF;
                IF JSON_LENGTH(operation_map) > 0 THEN
                    SET converted = JSON_SET(converted, CONCAT('$.', operation_name), operation_map);
                END IF;
                SET section_index = section_index + 1;
            END WHILE;
            IF NOT (original <=> converted) THEN
                UPDATE iam_default_policy_revision SET definition = converted WHERE id = row_id;
                INSERT INTO iam_field_access_016_changed_defaults VALUES (row_id);
                SET changed_defaults = changed_defaults + 1;
            END IF;
        END LOOP;
        CLOSE defaults_cursor;
    END;

    -- 4. 留档旧行级编辑规则，并转为同查看者的成员UPDATE全局规则。
    -- ALL/空范围且原FULL、editable=true才能迁移true；有限目标规则关闭编辑。
    IF has_legacy_editable THEN
        BEGIN
            DECLARE finished BOOLEAN DEFAULT FALSE;
            DECLARE row_id BIGINT UNSIGNED;
            DECLARE tenant_key BIGINT UNSIGNED;
            DECLARE viewer_key BIGINT UNSIGNED;
            DECLARE scenario_value VARCHAR(16);
            DECLARE field_key VARCHAR(128);
            DECLARE target_scopes JSON;
            DECLARE bindings JSON;
            DECLARE visibility_value VARCHAR(8);
            DECLARE editable_value BOOLEAN;
            DECLARE target_all BOOLEAN;
            DECLARE migrated_editable BOOLEAN;
            DECLARE member_ids JSON;
            DECLARE department_ids JSON;
            DECLARE viewer JSON;
            DECLARE operation_rule JSON;
            DECLARE rules_cursor CURSOR FOR SELECT r.id, r.tenant_id, r.viewer_selector_id, r.scenario,
                r.field_key, r.target_scope, r.scope_bindings, r.visibility, r.editable FROM iam_field_rule r;
            DECLARE CONTINUE HANDLER FOR NOT FOUND SET finished = TRUE;
            OPEN rules_cursor;
            rules_loop: LOOP
                FETCH rules_cursor INTO row_id, tenant_key, viewer_key, scenario_value, field_key,
                    target_scopes, bindings, visibility_value, editable_value;
                IF finished THEN LEAVE rules_loop; END IF;
                SELECT COALESCE(JSON_ARRAYAGG(CAST(member_id AS CHAR)), JSON_ARRAY()) INTO member_ids
                    FROM iam_policy_selector_member WHERE tenant_id = tenant_key AND selector_id = viewer_key;
                SELECT COALESCE(JSON_ARRAYAGG(JSON_OBJECT('id', CAST(department_id AS CHAR),
                    'includeDescendants', JSON_EXTRACT(IF(include_descendants, 'true', 'false'), '$'))), JSON_ARRAY())
                    INTO department_ids FROM iam_policy_selector_department WHERE tenant_id = tenant_key AND selector_id = viewer_key;
                SET viewer = JSON_OBJECT('members', member_ids, 'departments', department_ids);
                SET target_all = JSON_LENGTH(target_scopes) = 0 OR JSON_CONTAINS(target_scopes, JSON_OBJECT('kind', 'ALL'));
                SET migrated_editable = scenario_value = 'MANAGEMENT' AND target_all
                    AND editable_value AND visibility_value = 'FULL';
                INSERT IGNORE INTO iam_field_access_016_legacy_rule VALUES
                    (row_id, tenant_key, scenario_value, field_key, viewer, target_scopes, bindings,
                        visibility_value, editable_value, migrated_editable, scenario_value = 'MANAGEMENT' AND NOT target_all);
                IF scenario_value = 'MANAGEMENT' THEN
                    SET operation_rule = JSON_OBJECT('resource', JSON_OBJECT('domain', 'TENANT',
                        'applicationCode', 'iam-tenant', 'resourceCode', 'member'),
                        'scenario', scenario_value, 'actionCode', 'iam-tenant:member:update', 'fieldKey', field_key,
                        'viewerSelection', viewer, 'operations', JSON_OBJECT(
                            'editable', JSON_EXTRACT(IF(migrated_editable, 'true', 'false'), '$'), 'filterable', JSON_EXTRACT('false', '$')));
                    -- 重跑不重复；与已有规则共同匹配时新框架取交集，旧限制不会被丢掉。
                    UPDATE iam_field_policy SET operation_rules = JSON_ARRAY_APPEND(operation_rules, '$', operation_rule),
                        version = version + 1 WHERE tenant_id = tenant_key AND NOT JSON_CONTAINS(operation_rules, operation_rule);
                    SET changed_operations = changed_operations + ROW_COUNT();
                END IF;
            END LOOP;
            CLOSE rules_cursor;
        END;
    END IF;
    UPDATE iam_field_policy p JOIN iam_field_access_016_changed_defaults d ON d.id = p.default_revision_id
        SET p.version = p.version + 1;
    COMMIT;

    -- DDL单独提交；保留可见性/目标范围/选择器，原editable已经留档。
    IF EXISTS(SELECT 1 FROM information_schema.table_constraints WHERE constraint_schema = DATABASE()
            AND table_name = 'iam_field_rule' AND constraint_name = 'ck_iam_field_rule_editable') THEN
        ALTER TABLE iam_field_rule DROP CHECK ck_iam_field_rule_editable;
    END IF;
    IF has_legacy_editable THEN ALTER TABLE iam_field_rule DROP COLUMN editable; END IF;
    DROP TEMPORARY TABLE iam_field_access_016_changed_defaults;

    SELECT DATABASE() AS migrated_database, changed_resources AS resources_converted,
        changed_roles AS role_revisions_converted, changed_defaults AS default_revisions_converted,
        changed_operations AS operation_rules_added;
    SELECT rule_id, tenant_id, field_key, viewer_selection, target_scope, legacy_editable, migrated_editable
        FROM iam_field_access_016_legacy_rule WHERE requires_review ORDER BY tenant_id, rule_id;
END$$
DELIMITER ;
CALL ingot_iam_migrate_field_access_016();
DROP PROCEDURE ingot_iam_migrate_field_access_016;
