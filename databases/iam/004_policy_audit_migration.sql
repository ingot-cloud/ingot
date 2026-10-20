-- 业务时间点统一 UTC；初始化连接也必须明确会话时区。
SET time_zone = '+00:00';

-- IAM 独立目标库：策略引用、审计事实与迁移记录，不初始化或授予任何默认权限。
CREATE TABLE iam_default_policy_revision (
    id BIGINT UNSIGNED NOT NULL COMMENT '默认通讯录或字段策略固定版本记录标识',
    kind VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '默认策略类别：DIRECTORY通讯录、FIELD字段',
    revision BIGINT UNSIGNED NOT NULL COMMENT '不可变快照的发布序号，从1开始；与记录乐观锁版本不同',
    definition JSON NOT NULL COMMENT '默认策略不可变定义JSON对象，由对应通讯录或字段求值器解释',
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_default_policy_kind (id, kind),
    UNIQUE KEY uk_iam_default_policy_revision (kind, revision),
    CONSTRAINT ck_iam_default_policy_kind CHECK (kind IN ('DIRECTORY', 'FIELD')),
    CONSTRAINT ck_iam_default_policy_definition CHECK (JSON_TYPE(definition) = 'OBJECT'),
    CONSTRAINT ck_iam_default_policy_revision CHECK (revision > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='默认通讯录或字段策略固定版本';

CREATE TABLE iam_policy_selector (
    id BIGINT UNSIGNED NOT NULL COMMENT '租户策略人员选择器记录标识',
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_selector_tenant (tenant_id, id),
    CONSTRAINT fk_iam_selector_tenant FOREIGN KEY (tenant_id) REFERENCES iam_tenant (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户策略人员选择器';

CREATE TABLE iam_policy_selector_member (
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    selector_id BIGINT UNSIGNED NOT NULL COMMENT '人员选择器标识；引用归属由本表外键及业务事务共同校验',
    member_id BIGINT UNSIGNED NOT NULL COMMENT '成员标识；引用归属由本表外键及业务事务共同校验',
    PRIMARY KEY (tenant_id, selector_id, member_id),
    CONSTRAINT fk_iam_selector_member_selector FOREIGN KEY (tenant_id, selector_id) REFERENCES iam_policy_selector (tenant_id, id),
    CONSTRAINT fk_iam_selector_member_member FOREIGN KEY (tenant_id, member_id) REFERENCES iam_tenant_member (tenant_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='策略选择器显式成员';

CREATE TABLE iam_policy_selector_department (
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    selector_id BIGINT UNSIGNED NOT NULL COMMENT '人员选择器标识；引用归属由本表外键及业务事务共同校验',
    department_id BIGINT UNSIGNED NOT NULL COMMENT '部门标识；引用归属由本表外键及业务事务共同校验',
    include_descendants BOOLEAN NOT NULL DEFAULT FALSE COMMENT '选择该部门时是否包含下级部门成员：1包含、0仅直属',
    PRIMARY KEY (tenant_id, selector_id, department_id),
    CONSTRAINT fk_iam_selector_dept_selector FOREIGN KEY (tenant_id, selector_id) REFERENCES iam_policy_selector (tenant_id, id),
    CONSTRAINT fk_iam_selector_dept_dept FOREIGN KEY (tenant_id, department_id) REFERENCES iam_department (tenant_id, id),
    CONSTRAINT ck_iam_selector_descendants CHECK (include_descendants IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='策略选择器部门来源';

CREATE TABLE iam_directory_policy (
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    default_revision_id BIGINT UNSIGNED NOT NULL COMMENT '绑定的默认策略固定版本ID，与default_kind共同校验引用',
    default_kind VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'DIRECTORY' COMMENT '默认策略种类，固定为当前策略表对应的DIRECTORY或FIELD',
    default_scope VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT '空表示继承固定默认版本',
    default_selector_id BIGINT UNSIGNED NULL COMMENT '默认范围为SELECTED时的人员选择器ID，其他情况为空',
    version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    PRIMARY KEY (tenant_id),
    CONSTRAINT fk_iam_directory_policy_tenant FOREIGN KEY (tenant_id) REFERENCES iam_tenant (id),
    CONSTRAINT fk_iam_directory_default FOREIGN KEY (default_revision_id, default_kind) REFERENCES iam_default_policy_revision (id, kind),
    CONSTRAINT fk_iam_directory_default_selector FOREIGN KEY (tenant_id, default_selector_id) REFERENCES iam_policy_selector (tenant_id, id),
    CONSTRAINT ck_iam_directory_default_kind CHECK (default_kind = 'DIRECTORY'),
    CONSTRAINT ck_iam_directory_default_scope CHECK (
        (default_scope IS NULL AND default_selector_id IS NULL) OR
        (default_scope IS NOT NULL AND ((default_scope IN ('ALL', 'SELF') AND default_selector_id IS NULL) OR
        (default_scope = 'SELECTED' AND default_selector_id IS NOT NULL))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户通讯录策略及默认版本引用';

CREATE TABLE iam_directory_rule (
    id BIGINT UNSIGNED NOT NULL COMMENT '租户通讯录允许或拒绝规则记录标识',
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    effect VARCHAR(8) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '通讯录规则效果：ALLOW允许、DENY拒绝；取值ALLOW,DENY',
    viewer_selector_id BIGINT UNSIGNED NOT NULL COMMENT '规则适用的查看者选择器ID，须属于本租户',
    target_selector_id BIGINT UNSIGNED NOT NULL COMMENT '通讯录可见目标的选择器ID，须属于本租户',
    PRIMARY KEY (id),
    KEY idx_iam_directory_rule (tenant_id, effect, id),
    CONSTRAINT fk_iam_directory_rule_policy FOREIGN KEY (tenant_id) REFERENCES iam_directory_policy (tenant_id),
    CONSTRAINT fk_iam_directory_rule_viewer FOREIGN KEY (tenant_id, viewer_selector_id) REFERENCES iam_policy_selector (tenant_id, id),
    CONSTRAINT fk_iam_directory_rule_target FOREIGN KEY (tenant_id, target_selector_id) REFERENCES iam_policy_selector (tenant_id, id),
    CONSTRAINT ck_iam_directory_effect CHECK (effect IN ('ALLOW', 'DENY'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户通讯录允许或拒绝规则';

CREATE TABLE iam_field_policy (
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    default_revision_id BIGINT UNSIGNED NOT NULL COMMENT '绑定的默认策略固定版本ID，与default_kind共同校验引用',
    default_kind VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'FIELD' COMMENT '默认策略种类，固定为当前策略表对应的DIRECTORY或FIELD',
    operation_rules JSON NOT NULL DEFAULT (JSON_ARRAY()) COMMENT '租户字段操作规则JSON数组，限定已接入的编辑及筛选等能力',
    version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    PRIMARY KEY (tenant_id),
    CONSTRAINT fk_iam_field_policy_tenant FOREIGN KEY (tenant_id) REFERENCES iam_tenant (id),
    CONSTRAINT fk_iam_field_default FOREIGN KEY (default_revision_id, default_kind) REFERENCES iam_default_policy_revision (id, kind),
    CONSTRAINT ck_iam_field_default_kind CHECK (default_kind = 'FIELD'),
    CONSTRAINT ck_iam_field_operations CHECK (JSON_TYPE(operation_rules) = 'ARRAY')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户字段策略及操作规则';

CREATE TABLE iam_field_rule (
    id BIGINT UNSIGNED NOT NULL COMMENT '租户场景字段可见性规则记录标识',
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    scenario VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '字段策略使用场景：MANAGEMENT管理、DIRECTORY通讯录；取值MANAGEMENT,DIRECTORY',
    field_key VARCHAR(128) COLLATE utf8mb4_bin NOT NULL COMMENT '资源字段稳定标识，用于匹配目录能力及字段规则',
    viewer_selector_id BIGINT UNSIGNED NOT NULL COMMENT '规则适用的查看者选择器ID，须属于本租户',
    target_scope JSON NOT NULL COMMENT '字段规则目标范围表达式JSON数组',
    scope_bindings JSON NOT NULL COMMENT '参数名到范围对象的绑定JSON对象；空对象表示无显式绑定',
    visibility VARCHAR(8) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '字段可见性：HIDDEN隐藏、MASKED脱敏、FULL完整；取值HIDDEN,MASKED,FULL',
    PRIMARY KEY (id),
    KEY idx_iam_field_rule_match (tenant_id, scenario, field_key, id),
    CONSTRAINT fk_iam_field_rule_policy FOREIGN KEY (tenant_id) REFERENCES iam_field_policy (tenant_id),
    CONSTRAINT fk_iam_field_rule_viewer FOREIGN KEY (tenant_id, viewer_selector_id) REFERENCES iam_policy_selector (tenant_id, id),
    CONSTRAINT ck_iam_field_rule_scenario CHECK (scenario IN ('MANAGEMENT', 'DIRECTORY')),
    CONSTRAINT ck_iam_field_rule_visibility CHECK (visibility IN ('HIDDEN', 'MASKED', 'FULL')),
    CONSTRAINT ck_iam_field_rule_scope CHECK (JSON_TYPE(target_scope) = 'ARRAY'),
    CONSTRAINT ck_iam_field_rule_bindings CHECK (JSON_TYPE(scope_bindings) = 'OBJECT')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户场景字段可见性规则';

-- 成员导出任务：共享库存储，供多实例读取状态、成员 ID 快照与过期清理。
CREATE TABLE iam_member_export (
    id BIGINT UNSIGNED NOT NULL COMMENT '租户成员导出任务与成员快照记录标识',
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    actor_member_id BIGINT UNSIGNED NOT NULL COMMENT '发起操作的当前域成员ID',
    tenant_version VARCHAR(32) COLLATE utf8mb4_bin NOT NULL COMMENT '登记导出任务时的组织版本，取iam_tenant.version',
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '当前业务记录状态，合法值由本表CHECK及对应业务枚举限定；取值PENDING,RUNNING,SUCCEEDED,FAILED,EXPIRED',
    member_ids JSON NULL COMMENT '导出成员ID的JSON快照数组；任务尚未生成快照时可空',
    failure_reason VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT '任务失败或阻塞的安全原因，不包含凭证及敏感原值',
    created_at DATETIME(6) NOT NULL COMMENT '记录创建时间点，UTC',
    completed_at DATETIME(6) NULL COMMENT '任务完成时间点，UTC；未完成时为空',
    expires_at DATETIME(6) NOT NULL COMMENT '到期时间点，UTC',
    PRIMARY KEY (id),
    KEY idx_iam_member_export_tenant_status (tenant_id, status, expires_at),
    CONSTRAINT ck_iam_member_export_status CHECK (status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'EXPIRED')),
    CONSTRAINT ck_iam_member_export_ids CHECK (member_ids IS NULL OR JSON_TYPE(member_ids) = 'ARRAY')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户成员导出任务与成员快照';

CREATE TABLE iam_authorization_audit (
    id BIGINT UNSIGNED NOT NULL COMMENT '授权与组织操作脱敏审计及投递状态记录标识',
    event_id VARCHAR(64) COLLATE utf8mb4_bin NOT NULL COMMENT '业务事件幂等标识，用于防止重复投递或重复保存',
    actor_account_id BIGINT UNSIGNED NOT NULL COMMENT '发起操作的全局账号ID',
    actor_member_id BIGINT UNSIGNED NOT NULL COMMENT '发起操作的当前域成员ID',
    domain VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '授权管理域：PLATFORM平台、TENANT租户',
    tenant_id BIGINT UNSIGNED NULL COMMENT '审计所属租户ID；平台域为空，租户域必填',
    target_type VARCHAR(64) COLLATE utf8mb4_bin NOT NULL COMMENT '审计或迁移目标的业务类型',
    target_id VARCHAR(128) COLLATE utf8mb4_bin NOT NULL COMMENT '目标业务标识，具体类型由target_type或entity_type解释',
    change_type VARCHAR(64) COLLATE utf8mb4_bin NOT NULL COMMENT '审计变更类型，对应AuditChangeType稳定字面量',
    safe_before JSON NOT NULL COMMENT '变更前脱敏摘要JSON对象，不保存凭证或敏感原值',
    safe_after JSON NOT NULL COMMENT '变更后脱敏摘要JSON对象，不保存凭证或敏感原值',
    revisions JSON NOT NULL COMMENT '本次变更涉及的对象版本JSON对象',
    delegation_id BIGINT UNSIGNED NULL COMMENT '委派标识；引用归属由本表外键及业务事务共同校验',
    assignment_id BIGINT UNSIGNED NULL COMMENT '角色分配标识；引用归属由本表外键及业务事务共同校验',
    trace_id VARCHAR(128) NULL COMMENT '请求链路追踪标识，可空',
    occurred_at DATETIME(6) NOT NULL COMMENT '业务发生时间点，UTC',
    delivered_at DATETIME(6) NULL COMMENT '审计投递完成时间点，UTC；NULL表示尚未确认投递',
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_audit_event (event_id),
    KEY idx_iam_audit_tenant_time (tenant_id, occurred_at, id),
    KEY idx_iam_audit_actor (actor_account_id, occurred_at, id),
    KEY idx_iam_audit_assignment_create (assignment_id, change_type, occurred_at, id),
    KEY idx_iam_audit_delivery (delivered_at, id),
    CONSTRAINT ck_iam_audit_context CHECK ((domain = 'PLATFORM' AND tenant_id IS NULL) OR (domain = 'TENANT' AND tenant_id IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='授权与组织操作脱敏审计及投递状态';

CREATE TABLE iam_migration_batch (
    id VARCHAR(64) COLLATE utf8mb4_bin NOT NULL COMMENT '历史身份迁移批次及验收状态记录标识',
    source_fingerprint CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '源数据结构或内容指纹，用于识别迁移输入',
    target_identifier VARCHAR(128) COLLATE utf8mb4_bin NOT NULL COMMENT '明确选择的迁移目标标识，防止误用目标',
    rules_version VARCHAR(64) COLLATE utf8mb4_bin NOT NULL COMMENT '本批次采用的迁移规则版本',
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'NEW' COMMENT '当前业务记录状态，合法值由本表CHECK及对应业务枚举限定；取值NEW,PREFLIGHTED,IMPORTED,VERIFIED,FAILED',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录创建时间点，UTC',
    verified_at DATETIME(6) NULL COMMENT '人工验收完成时间点，UTC；仅VERIFIED状态必填',
    PRIMARY KEY (id),
    CONSTRAINT ck_iam_batch_status CHECK (status IN ('NEW', 'PREFLIGHTED', 'IMPORTED', 'VERIFIED', 'FAILED')),
    CONSTRAINT ck_iam_batch_verified CHECK ((status = 'VERIFIED' AND verified_at IS NOT NULL) OR (status <> 'VERIFIED' AND verified_at IS NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='历史身份迁移批次及验收状态';

CREATE TABLE iam_migration_mapping (
    batch_id VARCHAR(64) COLLATE utf8mb4_bin NOT NULL COMMENT '迁移批次标识；引用归属由本表外键及业务事务共同校验',
    source_table VARCHAR(64) COLLATE utf8mb4_bin NOT NULL COMMENT '原始来源表名',
    source_id VARCHAR(128) COLLATE utf8mb4_bin NOT NULL COMMENT '原始记录标识',
    target_type VARCHAR(64) COLLATE utf8mb4_bin NOT NULL COMMENT '审计或迁移目标的业务类型',
    target_id VARCHAR(128) COLLATE utf8mb4_bin NOT NULL COMMENT '目标业务标识，具体类型由target_type或entity_type解释',
    disposition VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '人工迁移处置：KEEP_MAPPED/NARROW/RECONFIGURE/ARCHIVE/SKIP；取值KEEP_MAPPED,NARROW,RECONFIGURE,ARCHIVE,SKIP',
    actor VARCHAR(128) NOT NULL COMMENT '作出处置的操作者标识',
    reason VARCHAR(1024) NOT NULL COMMENT '明确的人工处置依据',
    PRIMARY KEY (batch_id, source_table, source_id, target_type, target_id),
    CONSTRAINT fk_iam_mapping_batch FOREIGN KEY (batch_id) REFERENCES iam_migration_batch (id),
    CONSTRAINT ck_iam_mapping_disposition CHECK (disposition IN ('KEEP_MAPPED', 'NARROW', 'RECONFIGURE', 'ARCHIVE', 'SKIP'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='历史标识到目标实体的人工处置映射';

CREATE TABLE iam_migration_issue (
    id BIGINT UNSIGNED NOT NULL COMMENT '历史迁移阻塞项及处置记录记录标识',
    batch_id VARCHAR(64) COLLATE utf8mb4_bin NOT NULL COMMENT '迁移批次标识；引用归属由本表外键及业务事务共同校验',
    issue_code VARCHAR(64) COLLATE utf8mb4_bin NOT NULL COMMENT '迁移问题稳定代码',
    source_table VARCHAR(64) COLLATE utf8mb4_bin NULL COMMENT '原始来源表名',
    source_id VARCHAR(128) COLLATE utf8mb4_bin NULL COMMENT '原始记录标识',
    safe_description VARCHAR(1024) NOT NULL COMMENT '迁移问题的脱敏说明',
    disposition VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT '人工迁移处置：KEEP_MAPPED/NARROW/RECONFIGURE/ARCHIVE/SKIP；取值KEEP_MAPPED,NARROW,RECONFIGURE,ARCHIVE,SKIP',
    resolved_by VARCHAR(128) NULL COMMENT '问题处置人；未处置时为空',
    resolution_reason VARCHAR(1024) NULL COMMENT '问题处置依据；未处置时为空',
    PRIMARY KEY (id),
    KEY idx_iam_issue_unresolved (batch_id, disposition, id),
    CONSTRAINT fk_iam_issue_batch FOREIGN KEY (batch_id) REFERENCES iam_migration_batch (id),
    CONSTRAINT ck_iam_issue_disposition CHECK (disposition IS NULL OR disposition IN ('KEEP_MAPPED', 'NARROW', 'RECONFIGURE', 'ARCHIVE', 'SKIP')),
    CONSTRAINT ck_iam_issue_resolution CHECK (
        (disposition IS NULL AND resolved_by IS NULL AND resolution_reason IS NULL) OR
        (disposition IS NOT NULL AND resolved_by IS NOT NULL AND resolution_reason IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='历史迁移阻塞项及处置记录';

-- 持久角色版本应用：平台域，明细不展开组成员。
CREATE TABLE IF NOT EXISTS iam_role_application_task (
    id BIGINT UNSIGNED NOT NULL PRIMARY KEY COMMENT '平台角色固定版本批量应用任务记录标识',
    role_id BIGINT UNSIGNED NOT NULL COMMENT '角色标识；引用归属由本表外键及业务事务共同校验',
    target_revision_id BIGINT UNSIGNED NOT NULL COMMENT '目标角色固定版本标识；引用归属由本表外键及业务事务共同校验',
    actor_account_id BIGINT UNSIGNED NOT NULL COMMENT '发起操作的全局账号ID',
    actor_member_id BIGINT UNSIGNED NOT NULL COMMENT '发起操作的当前域成员ID',
    created_by BIGINT UNSIGNED NOT NULL COMMENT '创建操作人标识',
    selection VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '版本应用选择方式：ALL/SOURCE_REVISIONS/ASSIGNMENTS；取值ALL,SOURCE_REVISIONS,ASSIGNMENTS',
    selection_snapshot JSON NOT NULL COMMENT '提交时冻结的分配选择条件JSON对象',
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '当前业务记录状态，合法值由本表CHECK及对应业务枚举限定；取值PREPARING,READY,BLOCKED,RUNNING,PAUSED,SUCCEEDED',
    version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    prepare_key VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '同角色任务准备幂等键',
    confirm_key VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT '任务确认幂等键；未确认时为空',
    lease_owner VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT '领取任务的节点标识；无租约时为空',
    lease_until DATETIME(6) NULL COMMENT '任务租约失效时间点，UTC；无租约时为空',
    failure_reason VARCHAR(512) NULL COMMENT '任务失败或阻塞的安全原因，不包含凭证及敏感原值',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录创建时间点，UTC',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录最后更新时间点，UTC',
    running_role_id BIGINT UNSIGNED GENERATED ALWAYS AS (CASE WHEN status='RUNNING' THEN role_id ELSE NULL END) STORED COMMENT '自动生成的运行角色键：RUNNING时取role_id，否则NULL；唯一索引限制同角色一个运行任务',
    UNIQUE KEY uk_iam_role_application_prepare(role_id,prepare_key),
    UNIQUE KEY uk_iam_role_application_running(running_role_id),
    KEY ix_iam_role_application_role(role_id,id),
    KEY ix_iam_role_application_claim(status,lease_until,id),
    CONSTRAINT fk_iam_role_application_role FOREIGN KEY(role_id) REFERENCES iam_role_definition(id),
    CONSTRAINT fk_iam_role_application_target FOREIGN KEY(target_revision_id) REFERENCES iam_role_revision(id),
    CONSTRAINT ck_iam_role_application_status CHECK(status IN ('PREPARING','READY','BLOCKED','RUNNING','PAUSED','SUCCEEDED')),
    CONSTRAINT ck_iam_role_application_selection CHECK(selection IN ('ALL','SOURCE_REVISIONS','ASSIGNMENTS'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台角色固定版本批量应用任务';

CREATE TABLE IF NOT EXISTS iam_role_application_item (
    id BIGINT UNSIGNED NOT NULL PRIMARY KEY COMMENT '角色版本应用的分配明细记录标识',
    task_id BIGINT UNSIGNED NOT NULL COMMENT '版本应用任务标识；引用归属由本表外键及业务事务共同校验',
    assignment_id BIGINT UNSIGNED NOT NULL COMMENT '角色分配标识；引用归属由本表外键及业务事务共同校验',
    original_snapshot JSON NOT NULL COMMENT '被选分配的原始快照JSON对象，用于预览与执行重验',
    expected_assignment_version BIGINT UNSIGNED NOT NULL COMMENT '执行前要求匹配的分配乐观锁版本',
    configured_bindings JSON NOT NULL COMMENT '任务内明确配置的范围参数JSON对象',
    preview_snapshot JSON NULL COMMENT '最近一次预览结果JSON快照；未预览时为空',
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '当前业务记录状态，合法值由本表CHECK及对应业务枚举限定；取值PENDING,BLOCKED,SUCCEEDED,EXCLUDED,REVOKED,EXPIRED,ALREADY_TARGET',
    failure_reason VARCHAR(512) NULL COMMENT '任务失败或阻塞的安全原因，不包含凭证及敏感原值',
    UNIQUE KEY uk_iam_role_application_assignment(task_id,assignment_id),
    KEY ix_iam_role_application_items(task_id,status,id),
    CONSTRAINT fk_iam_role_application_item_task FOREIGN KEY(task_id) REFERENCES iam_role_application_task(id),
    CONSTRAINT fk_iam_role_application_item_assignment FOREIGN KEY(assignment_id) REFERENCES iam_role_assignment(id),
    CONSTRAINT ck_iam_role_application_item_status CHECK(status IN ('PENDING','BLOCKED','SUCCEEDED','EXCLUDED','REVOKED','EXPIRED','ALREADY_TARGET'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色版本应用的分配明细';

CREATE TABLE iam_live_authorization_batch (
 batch_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY COMMENT '迁移批次标识；引用归属由本表外键及业务事务共同校验',
 source_digest CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '即时授权迁移前的源内容摘要',
 reverse_digest CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT '反向迁移核验摘要；尚未生成时为空',
 state VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '迁移方向状态：FORWARD正向、REVERSE反向；取值FORWARD,REVERSE',
 decisions JSON NOT NULL DEFAULT (JSON_ARRAY()) COMMENT '已确认迁移处置决策JSON数组',
 applied_at DATETIME(6) NULL COMMENT '迁移应用时间点，UTC；尚未应用时为空',
 CONSTRAINT ck_iam_live_batch_state CHECK(state IN ('FORWARD','REVERSE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户即时授权模型迁移批次';
CREATE TABLE iam_live_authorization_mapping (
 batch_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '迁移批次标识；引用归属由本表外键及业务事务共同校验',
 entity_type VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '迁移记录对应的实体类型',
 entity_id BIGINT UNSIGNED NOT NULL COMMENT '迁移记录对应的实体标识',
 source_row JSON NOT NULL COMMENT '迁移前原行JSON快照，仅供受控迁移与回退使用',
 target_id BIGINT UNSIGNED NULL COMMENT '目标业务标识，具体类型由target_type或entity_type解释',
 PRIMARY KEY(batch_id,entity_type,entity_id),
 FOREIGN KEY(batch_id) REFERENCES iam_live_authorization_batch(batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户即时授权迁移原行快照与目标映射';
