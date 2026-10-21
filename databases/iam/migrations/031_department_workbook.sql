-- 已有库补齐部门表格导入任务与导出操作。新建库使用 004/006，不执行本补丁。
CREATE TABLE IF NOT EXISTS iam_department_workbook (
    id BIGINT UNSIGNED NOT NULL COMMENT '租户部门表格导入任务标识',
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    actor_member_id BIGINT UNSIGNED NOT NULL COMMENT '发起操作的当前域成员ID',
    kind VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '任务种类；取值IMPORT',
    mode VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '表格模式；取值CREATE,UPDATE',
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '当前业务记录状态，合法值由本表CHECK及对应业务枚举限定；取值PENDING,RUNNING,SUCCEEDED,FAILED,EXPIRED',
    tenant_version VARCHAR(32) COLLATE utf8mb4_bin NOT NULL COMMENT '登记任务时的组织版本，取iam_tenant.version',
    department_ids JSON NULL COMMENT '导入预留范围；可空',
    result JSON NULL COMMENT '成功失败计数与逐行原因',
    workbook_path VARCHAR(512) COLLATE utf8mb4_bin NULL COMMENT '原始工作簿对象路径bucket/objectName',
    error_report_path VARCHAR(512) COLLATE utf8mb4_bin NULL COMMENT '错误报告对象路径bucket/objectName；无失败时为空',
    failure_reason VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT '整份任务失败的安全原因，不包含凭证及敏感原值',
    created_at DATETIME(6) NOT NULL COMMENT '记录创建时间点，UTC',
    completed_at DATETIME(6) NULL COMMENT '任务完成时间点，UTC；未完成时为空',
    expires_at DATETIME(6) NOT NULL COMMENT '到期时间点，UTC',
    PRIMARY KEY (id),
    KEY idx_iam_department_workbook_actor (tenant_id, actor_member_id, created_at),
    KEY idx_iam_department_workbook_status (tenant_id, status, expires_at),
    CONSTRAINT ck_iam_department_workbook_kind CHECK (kind IN ('IMPORT')),
    CONSTRAINT ck_iam_department_workbook_mode CHECK (mode IN ('CREATE', 'UPDATE')),
    CONSTRAINT ck_iam_department_workbook_status CHECK (status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'EXPIRED')),
    CONSTRAINT ck_iam_department_workbook_ids CHECK (department_ids IS NULL OR JSON_TYPE(department_ids) = 'ARRAY')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户部门表格导入任务';

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 121200 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 121200)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:department:export', '导出部门', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'department' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:department:export');

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:department:export' AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);
