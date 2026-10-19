-- 未上线开发环境专用：021已完整执行、022失败后，在选中的IAM库整份运行。
-- 停止IAM/授权写任务后执行；无需再次执行021、022或迁移导出工具。
-- 保留平台自有数据、组织/成员/应用配置及共享权限；停用旧租户授权。
-- 旧私有角色建立空权限，新模型已建立的角色/分配/委派重跑不重置。
SET NAMES utf8mb4;
SET time_zone = '+00:00';

DROP PROCEDURE IF EXISTS iam_prepare_tenant_live_test;
DELIMITER $$
CREATE PROCEDURE iam_prepare_tenant_live_test()
BEGIN
    DECLARE finished BOOLEAN DEFAULT FALSE;
    DECLARE role_identity BIGINT UNSIGNED;
    DECLARE role_type VARCHAR(24);
    DECLARE source_snapshot BIGINT UNSIGNED;
    DECLARE next_snapshot BIGINT UNSIGNED;
    DECLARE next_sequence BIGINT UNSIGNED;
    DECLARE legacy_roles CURSOR FOR
        SELECT id,kind FROM iam_role_definition
        WHERE domain='TENANT' AND kind IN ('SHARED','TENANT_CUSTOM')
          AND current_revision_id IS NULL ORDER BY id;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET finished=TRUE;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;

    IF DATABASE() IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='请先选中IAM数据库再执行整个脚本';
    END IF;
    IF (SELECT COUNT(*) FROM information_schema.columns
        WHERE table_schema=DATABASE() AND
          ((table_name='iam_role_definition' AND column_name='current_revision_id') OR
           (table_name='iam_role_parameter' AND column_name='resource_id') OR
           (table_name='iam_role_assignment' AND column_name IN ('role_id','role_kind','role_tenant_key'))))<>5
       OR (SELECT COUNT(*) FROM information_schema.tables
           WHERE table_schema=DATABASE() AND table_name IN ('iam_delegation_role',
               'iam_delegation_resource_ceiling','iam_live_authorization_batch','iam_live_authorization_mapping'))<>4 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='021结构未完整执行，本脚本不会重复添加021结构';
    END IF;
    IF EXISTS(SELECT 1 FROM iam_action old_code JOIN iam_action new_code
        ON (old_code.code='iam-tenant:role:publish' AND new_code.code='iam-tenant:role:update')
        OR (old_code.code='iam-platform:shared-role:publish' AND new_code.code='iam-platform:shared-role:update')) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='publish/update两个权限码同时存在，须先核对重复目录';
    END IF;

    START TRANSACTION;
    -- 仅退出旧固定版本租户授权，保留历史引用；新模型分配不改变。
    UPDATE iam_role_assignment SET status='REVOKED',version=version+1
    WHERE domain='TENANT' AND role_id IS NULL AND status='ACTIVE';
    UPDATE iam_delegation_grant source SET status='REVOKED',version=version+1
    WHERE source.domain='TENANT' AND source.status='ACTIVE'
      AND NOT EXISTS(SELECT 1 FROM iam_delegation_role current_role
          WHERE current_role.delegation_id=source.id);
    UPDATE iam_role_application_task task JOIN iam_role_definition role ON role.id=task.role_id
    SET task.status='BLOCKED',task.lease_owner=NULL,task.lease_until=NULL,
        task.failure_reason='未上线租户测试准备：旧版本任务停止',
        task.version=task.version+1,task.updated_at=CURRENT_TIMESTAMP(6)
    WHERE role.domain='TENANT' AND task.status IN ('PREPARING','READY','RUNNING','PAUSED');

    SELECT COALESCE(MAX(id),0)+1 INTO next_snapshot FROM iam_role_revision;
    OPEN legacy_roles;
    role_loop: LOOP
        FETCH legacy_roles INTO role_identity,role_type;
        IF finished THEN LEAVE role_loop; END IF;
        SELECT MAX(CASE WHEN revision=(SELECT MAX(revision) FROM iam_role_revision
                                      WHERE role_id=role_identity) THEN id END),
               COALESCE(MAX(revision),0)+1
        INTO source_snapshot,next_sequence FROM iam_role_revision WHERE role_id=role_identity;

        INSERT INTO iam_role_revision(id,role_id,kind,revision,metadata_overrides,resource_field_permissions)
        VALUES(next_snapshot,role_identity,role_type,next_sequence,JSON_OBJECT(),JSON_OBJECT());
        IF role_type='SHARED' AND source_snapshot IS NOT NULL THEN
            -- 保留共享角色操作与范围；旧跨资源参数键改成各资源独立的当前键。
            INSERT INTO iam_role_grant(revision_id,action_id,scopes)
            SELECT next_snapshot,grant_row.action_id,
                CASE WHEN JSON_LENGTH(grant_row.scopes)=0 THEN JSON_ARRAY() ELSE
                    (SELECT JSON_ARRAYAGG(CASE
                        WHEN scope_row.kind IN ('MANAGED_DEPARTMENTS','OBJECT_SET') THEN
                            JSON_SET(scope_row.expression,'$.parameterKey',
                                CONCAT('resource-',action.resource_id,'-',
                                    CASE scope_row.kind WHEN 'MANAGED_DEPARTMENTS' THEN 'departments' ELSE 'objects' END))
                        ELSE scope_row.expression END)
                     FROM JSON_TABLE(grant_row.scopes,'$[*]' COLUMNS(
                         expression JSON PATH '$',kind VARCHAR(32) PATH '$.kind')) scope_row)
                END
            FROM iam_role_grant grant_row JOIN iam_action action ON action.id=grant_row.action_id
            WHERE grant_row.revision_id=source_snapshot;
            INSERT INTO iam_role_parameter(revision_id,parameter_key,resource_id,binding_kind)
            SELECT DISTINCT next_snapshot,scope_row.parameter_key,action.resource_id,
                CASE scope_row.kind WHEN 'MANAGED_DEPARTMENTS' THEN 'DEPARTMENTS' ELSE 'OBJECTS' END
            FROM iam_role_grant grant_row JOIN iam_action action ON action.id=grant_row.action_id
            JOIN JSON_TABLE(grant_row.scopes,'$[*]' COLUMNS(
                kind VARCHAR(32) PATH '$.kind',parameter_key VARCHAR(128) PATH '$.parameterKey')) scope_row
            WHERE grant_row.revision_id=next_snapshot AND scope_row.kind IN ('MANAGED_DEPARTMENTS','OBJECT_SET');
        END IF;
        -- 私有角色不迁移基础/差异，建立空的独立当前定义供界面重新配置。
        UPDATE iam_role_definition SET current_revision_id=next_snapshot,version=version+1
        WHERE id=role_identity AND domain='TENANT';
        SET next_snapshot=next_snapshot+1;
    END LOOP;
    CLOSE legacy_roles;

    UPDATE iam_action SET code='iam-tenant:role:update',name='修改角色权限',version=version+1
    WHERE code='iam-tenant:role:publish';
    UPDATE iam_action SET code='iam-platform:shared-role:update',name='修改共享角色权限',version=version+1
    WHERE code='iam-platform:shared-role:publish';
    DELETE relation FROM iam_menu_action relation JOIN iam_action action ON action.id=relation.action_id
    WHERE action.code='iam-tenant:role:upgrade';
    UPDATE iam_action SET enabled=FALSE,version=version+1
    WHERE code='iam-tenant:role:upgrade' AND enabled=TRUE;
    COMMIT;

    -- 022是独立DDL检查点；上次失败未安装，已安装时跳过，整份脚本可重跑。
    IF NOT EXISTS(SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema=DATABASE() AND table_name='iam_role_assignment'
          AND constraint_name='ck_iam_assignment_current_role') THEN
        ALTER TABLE iam_role_assignment ADD CONSTRAINT ck_iam_assignment_current_role CHECK (
            (domain='PLATFORM' AND role_id IS NULL AND revision_id IS NOT NULL AND revision_kind IS NOT NULL) OR
            (domain='TENANT' AND role_id IS NOT NULL AND role_kind IS NOT NULL AND role_tenant_key IS NOT NULL AND
              ((role_kind='SHARED' AND role_tenant_key=0) OR
               (role_kind='TENANT_CUSTOM' AND role_tenant_key=tenant_id))) OR
            (domain='TENANT' AND status='REVOKED' AND role_id IS NULL AND revision_id IS NOT NULL));
    END IF;
    SELECT 'TENANT_LIVE_TEST_READY' AS result,
        (SELECT COUNT(*) FROM iam_role_definition WHERE domain='TENANT'
            AND kind IN ('SHARED','TENANT_CUSTOM') AND current_revision_id IS NOT NULL) AS current_tenant_roles,
        (SELECT COUNT(*) FROM iam_role_assignment WHERE domain='TENANT'
            AND role_id IS NULL AND status='ACTIVE') AS active_legacy_assignments;
END$$
DELIMITER ;
CALL iam_prepare_tenant_live_test();
DROP PROCEDURE iam_prepare_tenant_live_test;
