-- 业务时间点统一 UTC；初始化连接也必须明确会话时区。
SET time_zone = '+00:00';

-- IAM 独立目标库：先执行 001_identity.sql。无源库写入或授权默认数据。
CREATE TABLE iam_application (
    id BIGINT UNSIGNED NOT NULL COMMENT '应用目录记录标识',
    code VARCHAR(64) COLLATE utf8mb4_bin NOT NULL COMMENT '业务稳定编码，唯一性由本表索引限定',
    domain VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '授权管理域：PLATFORM平台、TENANT租户',
    name VARCHAR(128) NOT NULL COMMENT '业务显示名称',
    description VARCHAR(512) NULL COMMENT '业务说明，可空',
    icon VARCHAR(512) NULL COMMENT '界面图标标识或配置值，按所属应用菜单契约解释',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '同级显示排序权重，由查询排序规则使用',
    baseline BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否为租户基础治理应用：1是、0否；基础应用开通不可关闭',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用：1启用、0停用；不代表已授予操作权限',
    version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_application_code (code),
    CONSTRAINT ck_iam_application_domain CHECK (domain IN ('PLATFORM', 'TENANT')),
    CONSTRAINT ck_iam_application_baseline CHECK (baseline IN (0, 1)),
    CONSTRAINT ck_iam_application_baseline_domain CHECK (baseline = FALSE OR domain = 'TENANT'),
    CONSTRAINT ck_iam_application_enabled CHECK (enabled IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='应用目录';

CREATE TABLE iam_resource (
    id BIGINT UNSIGNED NOT NULL COMMENT '应用资源及范围字段能力记录标识',
    application_id BIGINT UNSIGNED NOT NULL COMMENT '应用标识；引用归属由本表外键及业务事务共同校验',
    code VARCHAR(64) COLLATE utf8mb4_bin NOT NULL COMMENT '业务稳定编码，唯一性由本表索引限定',
    name VARCHAR(128) NOT NULL COMMENT '业务显示名称',
    scope_capabilities JSON NOT NULL COMMENT '资源支持的范围能力JSON数组，作为授权范围校验上限',
    field_capabilities JSON NOT NULL COMMENT '资源字段能力JSON数组，限制可见性、编辑及筛选等可声明能力',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用：1启用、0停用；不代表已授予操作权限',
    version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_resource_code (application_id, code),
    UNIQUE KEY uk_iam_resource_application (application_id, id),
    CONSTRAINT fk_iam_resource_application FOREIGN KEY (application_id) REFERENCES iam_application (id),
    CONSTRAINT ck_iam_resource_scopes CHECK (JSON_TYPE(scope_capabilities) = 'ARRAY'),
    CONSTRAINT ck_iam_resource_fields CHECK (JSON_TYPE(field_capabilities) = 'ARRAY'),
    CONSTRAINT ck_iam_resource_enabled CHECK (enabled IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='应用资源及范围字段能力';

CREATE TABLE iam_action (
    id BIGINT UNSIGNED NOT NULL COMMENT '资源精确操作目录记录标识',
    application_id BIGINT UNSIGNED NOT NULL COMMENT '应用标识；引用归属由本表外键及业务事务共同校验',
    resource_id BIGINT UNSIGNED NOT NULL COMMENT '资源标识；引用归属由本表外键及业务事务共同校验',
    code VARCHAR(192) COLLATE utf8mb4_bin NOT NULL COMMENT '全局唯一精确操作码，禁止通配符，例如iam-tenant:member:read',
    name VARCHAR(128) NOT NULL COMMENT '业务显示名称',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用：1启用、0停用；不代表已授予操作权限',
    version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_action_code (code),
    UNIQUE KEY uk_iam_action_application (application_id, id),
    KEY idx_iam_action_resource (application_id, resource_id, id),
    CONSTRAINT fk_iam_action_resource FOREIGN KEY (application_id, resource_id) REFERENCES iam_resource (application_id, id),
    CONSTRAINT ck_iam_action_exact CHECK (LOCATE('*', code) = 0 AND CHAR_LENGTH(code) > 0),
    CONSTRAINT ck_iam_action_enabled CHECK (enabled IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资源精确操作目录';

CREATE TABLE iam_menu (
    id BIGINT UNSIGNED NOT NULL COMMENT '应用菜单与前端路由记录标识',
    application_id BIGINT UNSIGNED NOT NULL COMMENT '应用标识；引用归属由本表外键及业务事务共同校验',
    parent_id BIGINT UNSIGNED NULL COMMENT '父节点ID；NULL表示根节点，必须属于相同归属域',
    name VARCHAR(128) NOT NULL COMMENT '业务显示名称',
    path VARCHAR(512) NULL COMMENT '前端菜单路由路径，可空',
    view_path VARCHAR(256) NULL COMMENT '前端视图注册键；目录布局使用约定布局键',
    route_name VARCHAR(128) NULL COMMENT '前端路由稳定名称，可空',
    icon VARCHAR(512) NULL COMMENT '界面图标标识或配置值，按所属应用菜单契约解释',
    kind VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '菜单类别：DIRECTORY目录、PAGE页面',
    match_mode VARCHAR(8) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ANY' COMMENT '菜单操作匹配方式：ANY任一、ALL全部；取值ANY,ALL',
    access_mode VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ACTION' COMMENT '菜单准入模式：OPEN开放、ACTION按绑定操作判断；取值OPEN,ACTION',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '同级显示排序权重，由查询排序规则使用',
    hidden BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否在菜单中隐藏：1隐藏、0展示；不授予访问权限',
    is_cache BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否启用前端页面缓存：1是、0否',
    props BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否将路由参数作为页面属性传入：1是、0否',
    route_params JSON NULL COMMENT '路由参数JSON，可空',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用：1启用、0停用；不代表已授予操作权限',
    version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_menu_application (application_id, id),
    KEY idx_iam_menu_parent (application_id, parent_id, sort_order, id),
    CONSTRAINT fk_iam_menu_application FOREIGN KEY (application_id) REFERENCES iam_application (id),
    CONSTRAINT fk_iam_menu_parent FOREIGN KEY (application_id, parent_id) REFERENCES iam_menu (application_id, id),
    CONSTRAINT ck_iam_menu_kind CHECK (kind IN ('DIRECTORY', 'PAGE')),
    CONSTRAINT ck_iam_menu_match CHECK (match_mode IN ('ANY', 'ALL')),
    CONSTRAINT ck_iam_menu_access CHECK (access_mode IN ('OPEN', 'ACTION')),
    CONSTRAINT ck_iam_menu_parent CHECK (parent_id IS NULL OR parent_id <> id),
    CONSTRAINT ck_iam_menu_enabled CHECK (enabled IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='应用菜单与前端路由';

CREATE TABLE iam_menu_action (
    application_id BIGINT UNSIGNED NOT NULL COMMENT '应用标识；引用归属由本表外键及业务事务共同校验',
    menu_id BIGINT UNSIGNED NOT NULL COMMENT '菜单标识；引用归属由本表外键及业务事务共同校验',
    action_id BIGINT UNSIGNED NOT NULL COMMENT '精确操作标识；引用归属由本表外键及业务事务共同校验',
    PRIMARY KEY (application_id, menu_id, action_id),
    KEY idx_iam_action_menus (application_id, action_id, menu_id),
    CONSTRAINT fk_iam_menu_action_menu FOREIGN KEY (application_id, menu_id) REFERENCES iam_menu (application_id, id),
    CONSTRAINT fk_iam_menu_action_action FOREIGN KEY (application_id, action_id) REFERENCES iam_action (application_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜单操作绑定';

CREATE TABLE iam_tenant_app_entitlement (
    id BIGINT UNSIGNED NOT NULL COMMENT '租户应用开通记录标识',
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    application_id BIGINT UNSIGNED NOT NULL COMMENT '应用标识；引用归属由本表外键及业务事务共同校验',
    enabled BOOLEAN NOT NULL COMMENT '是否启用：1启用、0停用；不代表已授予操作权限',
    source VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '记录来源，取值及对应来源标识由所属业务契约解释；取值INITIALIZATION,MANUAL,PLAN,MIGRATION',
    source_id BIGINT UNSIGNED NULL COMMENT '开通来源对象ID；PLAN来源记录套餐ID，其他来源按业务规则可空',
    valid_from DATETIME(6) NULL COMMENT '生效时间点，UTC；可空时表示不限制开始时间',
    valid_until DATETIME(6) NULL COMMENT '失效时间点，UTC；NULL表示不限制结束时间',
    version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_entitlement (tenant_id, application_id),
    CONSTRAINT fk_iam_entitlement_tenant FOREIGN KEY (tenant_id) REFERENCES iam_tenant (id),
    CONSTRAINT fk_iam_entitlement_app FOREIGN KEY (application_id) REFERENCES iam_application (id),
    CONSTRAINT ck_iam_entitlement_source CHECK (source IN ('INITIALIZATION', 'MANUAL', 'PLAN', 'MIGRATION')),
    CONSTRAINT ck_iam_entitlement_interval CHECK (valid_from IS NULL OR valid_until IS NULL OR valid_from < valid_until),
    CONSTRAINT ck_iam_entitlement_enabled CHECK (enabled IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户应用开通';

CREATE TABLE iam_app_audience (
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    application_id BIGINT UNSIGNED NOT NULL COMMENT '应用标识；引用归属由本表外键及业务事务共同校验',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用：1启用、0停用；不代表已授予操作权限',
    audience_kind VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '应用可用人群：ALL全部成员、SELECTED指定来源；开通不等于操作授权；取值ALL,SELECTED',
    version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    PRIMARY KEY (tenant_id, application_id),
    CONSTRAINT fk_iam_audience_entitlement FOREIGN KEY (tenant_id, application_id) REFERENCES iam_tenant_app_entitlement (tenant_id, application_id),
    CONSTRAINT ck_iam_audience_kind CHECK (audience_kind IN ('ALL', 'SELECTED')),
    CONSTRAINT ck_iam_audience_enabled CHECK (enabled IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户应用可用人群配置';

CREATE TABLE iam_audience_member (
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    application_id BIGINT UNSIGNED NOT NULL COMMENT '应用标识；引用归属由本表外键及业务事务共同校验',
    member_id BIGINT UNSIGNED NOT NULL COMMENT '成员标识；引用归属由本表外键及业务事务共同校验',
    PRIMARY KEY (tenant_id, application_id, member_id),
    CONSTRAINT fk_iam_audience_member_config FOREIGN KEY (tenant_id, application_id) REFERENCES iam_app_audience (tenant_id, application_id),
    CONSTRAINT fk_iam_audience_member FOREIGN KEY (tenant_id, member_id) REFERENCES iam_tenant_member (tenant_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='应用人群显式成员';

CREATE TABLE iam_audience_department (
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    application_id BIGINT UNSIGNED NOT NULL COMMENT '应用标识；引用归属由本表外键及业务事务共同校验',
    department_id BIGINT UNSIGNED NOT NULL COMMENT '部门标识；引用归属由本表外键及业务事务共同校验',
    include_descendants BOOLEAN NOT NULL DEFAULT FALSE COMMENT '选择该部门时是否包含下级部门成员：1包含、0仅直属',
    PRIMARY KEY (tenant_id, application_id, department_id),
    CONSTRAINT fk_iam_audience_dept_config FOREIGN KEY (tenant_id, application_id) REFERENCES iam_app_audience (tenant_id, application_id),
    CONSTRAINT fk_iam_audience_dept FOREIGN KEY (tenant_id, department_id) REFERENCES iam_department (tenant_id, id),
    CONSTRAINT ck_iam_audience_descendants CHECK (include_descendants IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='应用人群部门来源';

CREATE TABLE iam_audience_group (
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    application_id BIGINT UNSIGNED NOT NULL COMMENT '应用标识；引用归属由本表外键及业务事务共同校验',
    group_id BIGINT UNSIGNED NOT NULL COMMENT '用户组标识；引用归属由本表外键及业务事务共同校验',
    PRIMARY KEY (tenant_id, application_id, group_id),
    CONSTRAINT fk_iam_audience_group_config FOREIGN KEY (tenant_id, application_id) REFERENCES iam_app_audience (tenant_id, application_id),
    CONSTRAINT fk_iam_audience_group FOREIGN KEY (tenant_id, group_id) REFERENCES iam_tenant_group (tenant_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='应用人群用户组来源';

CREATE TABLE iam_plan (
    id BIGINT UNSIGNED NOT NULL COMMENT '套餐目录记录标识',
    name VARCHAR(128) NOT NULL COMMENT '业务显示名称',
    description VARCHAR(512) NULL COMMENT '业务说明，可空',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用：1启用、0停用；不代表已授予操作权限',
    version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    PRIMARY KEY (id),
    CONSTRAINT ck_iam_plan_enabled CHECK (enabled IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='套餐目录';

CREATE TABLE iam_plan_application (
    plan_id BIGINT UNSIGNED NOT NULL COMMENT '套餐标识；引用归属由本表外键及业务事务共同校验',
    application_id BIGINT UNSIGNED NOT NULL COMMENT '应用标识；引用归属由本表外键及业务事务共同校验',
    PRIMARY KEY (plan_id, application_id),
    CONSTRAINT fk_iam_plan_application_plan FOREIGN KEY (plan_id) REFERENCES iam_plan (id),
    CONSTRAINT fk_iam_plan_application_app FOREIGN KEY (application_id) REFERENCES iam_application (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='套餐包含的应用';

CREATE TABLE iam_role_definition (
    id BIGINT UNSIGNED NOT NULL COMMENT '角色身份与当前快照指针记录标识',
    domain VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '授权管理域：PLATFORM平台、TENANT租户',
    tenant_id BIGINT UNSIGNED NULL COMMENT '所属租户组织ID；NULL表示平台域或共享定义',
    tenant_key BIGINT UNSIGNED GENERATED ALWAYS AS (COALESCE(tenant_id, 0)) STORED COMMENT '自动生成的归属唯一键：tenant_id为空取0，否则取租户ID；用于复合唯一索引及外键',
    kind VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '角色类别：SYSTEM系统、SHARED共享、PLATFORM_CUSTOM平台自有、TENANT_CUSTOM租户私有',
    code VARCHAR(128) COLLATE utf8mb4_bin NOT NULL COMMENT '业务稳定编码，唯一性由本表索引限定',
    name VARCHAR(128) NOT NULL COMMENT '业务显示名称',
    description VARCHAR(512) NULL COMMENT '业务说明，可空',
    group_name VARCHAR(128) NULL COMMENT '角色展示分组名称，可空',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用：1启用、0停用；不代表已授予操作权限',
    version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    current_revision_id BIGINT UNSIGNED NULL COMMENT '共享或租户私有角色当前完整权限快照ID；平台固定版本路径不依赖此指针',
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_role_code (domain, tenant_key, code),
    UNIQUE KEY uk_iam_role_kind (id, kind),
    UNIQUE KEY uk_iam_role_owner (domain, tenant_key, id, kind),
    CONSTRAINT fk_iam_role_tenant FOREIGN KEY (tenant_id) REFERENCES iam_tenant (id),
    CONSTRAINT ck_iam_role_domain CHECK (domain IN ('PLATFORM', 'TENANT')),
    CONSTRAINT ck_iam_role_kind CHECK (
        (kind = 'SYSTEM' AND tenant_id IS NULL) OR
        (kind = 'SHARED' AND domain = 'TENANT' AND tenant_id IS NULL) OR
        (kind = 'PLATFORM_CUSTOM' AND domain = 'PLATFORM' AND tenant_id IS NULL) OR
        (kind = 'TENANT_CUSTOM' AND domain = 'TENANT' AND tenant_id IS NOT NULL)),
    CONSTRAINT ck_iam_role_enabled CHECK (enabled IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色身份与当前快照指针';

CREATE TABLE iam_role_revision (
    id BIGINT UNSIGNED NOT NULL COMMENT '角色不可变权限快照与可编辑版本元数据记录标识',
    role_id BIGINT UNSIGNED NOT NULL COMMENT '角色标识；引用归属由本表外键及业务事务共同校验',
    kind VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '权限快照所属角色类别，与role_id共同引用角色；快照权限内容不可变',
    revision BIGINT UNSIGNED NOT NULL COMMENT '不可变快照的发布序号，从1开始；与记录乐观锁版本不同',
    base_revision_id BIGINT UNSIGNED NULL COMMENT '历史定制角色引用的共享基础版本ID；NULL表示无基础版本',
    base_kind VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin
        GENERATED ALWAYS AS (CASE WHEN base_revision_id IS NULL THEN NULL ELSE 'SHARED' END) STORED COMMENT '自动生成的基础版本种类：有base_revision_id时为SHARED，否则NULL，用于复合外键',
    metadata_overrides JSON NOT NULL COMMENT '角色快照元数据覆盖JSON对象；空对象表示无覆盖',
    resource_field_permissions JSON NOT NULL DEFAULT (JSON_OBJECT()) COMMENT '固定资源字段权限快照，未声明字段不授予权限',
    version_name VARCHAR(128) NULL COMMENT '版本显示名称；空值回退vN',
    remark VARCHAR(512) NULL COMMENT '版本说明',
    metadata_version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '独立元数据乐观锁',
    published_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '权限快照发布时间点，UTC',
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_revision_owner (id, role_id),
    UNIQUE KEY uk_iam_revision_number (role_id, revision),
    UNIQUE KEY uk_iam_revision_kind (id, kind),
    CONSTRAINT fk_iam_revision_role FOREIGN KEY (role_id, kind) REFERENCES iam_role_definition (id, kind),
    CONSTRAINT fk_iam_revision_base FOREIGN KEY (base_revision_id, base_kind) REFERENCES iam_role_revision (id, kind),
    CONSTRAINT ck_iam_revision_number CHECK (revision > 0),
    CONSTRAINT ck_iam_revision_base CHECK (base_revision_id IS NULL OR (kind = 'TENANT_CUSTOM' AND base_revision_id <> id)),
    CONSTRAINT ck_iam_revision_metadata CHECK (JSON_TYPE(metadata_overrides) = 'OBJECT'),
    CONSTRAINT ck_iam_revision_fields CHECK (JSON_TYPE(resource_field_permissions) = 'OBJECT')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色不可变权限快照与可编辑版本元数据';

CREATE TABLE iam_role_parameter (
    revision_id BIGINT UNSIGNED NOT NULL COMMENT '角色固定版本标识；引用归属由本表外键及业务事务共同校验',
    parameter_key VARCHAR(128) COLLATE utf8mb4_bin NOT NULL COMMENT '快照内唯一的范围参数名称，用于分配参数绑定',
    resource_id BIGINT UNSIGNED NULL COMMENT '资源标识；引用归属由本表外键及业务事务共同校验',
    binding_kind VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '范围参数绑定类别：DEPARTMENTS部门、OBJECTS业务对象；取值DEPARTMENTS,OBJECTS',
    PRIMARY KEY (revision_id, parameter_key),
    CONSTRAINT fk_iam_parameter_revision FOREIGN KEY (revision_id) REFERENCES iam_role_revision (id),
    CONSTRAINT fk_iam_parameter_resource FOREIGN KEY(resource_id) REFERENCES iam_resource(id),
    CONSTRAINT ck_iam_parameter_kind CHECK (binding_kind IN ('DEPARTMENTS', 'OBJECTS'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色快照范围参数定义';

CREATE TABLE iam_role_grant (
    revision_id BIGINT UNSIGNED NOT NULL COMMENT '角色固定版本标识；引用归属由本表外键及业务事务共同校验',
    action_id BIGINT UNSIGNED NOT NULL COMMENT '精确操作标识；引用归属由本表外键及业务事务共同校验',
    scopes JSON NOT NULL COMMENT '授权或上限的范围表达式JSON数组；具体空数组语义由对应求值器判定',
    PRIMARY KEY (revision_id, action_id),
    CONSTRAINT fk_iam_grant_revision FOREIGN KEY (revision_id) REFERENCES iam_role_revision (id),
    CONSTRAINT fk_iam_grant_action FOREIGN KEY (action_id) REFERENCES iam_action (id),
    CONSTRAINT ck_iam_grant_scopes CHECK (JSON_TYPE(scopes) = 'ARRAY')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色快照操作授权';

CREATE TABLE iam_role_delta (
    revision_id BIGINT UNSIGNED NOT NULL COMMENT '角色固定版本标识；引用归属由本表外键及业务事务共同校验',
    action_id BIGINT UNSIGNED NOT NULL COMMENT '精确操作标识；引用归属由本表外键及业务事务共同校验',
    operation VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '历史定制差异操作：ADD增加、REMOVE移除、REPLACE_SCOPE替换范围',
    scopes JSON NOT NULL COMMENT '授权或上限的范围表达式JSON数组；具体空数组语义由对应求值器判定',
    PRIMARY KEY (revision_id, action_id),
    CONSTRAINT fk_iam_delta_revision FOREIGN KEY (revision_id) REFERENCES iam_role_revision (id),
    CONSTRAINT fk_iam_delta_action FOREIGN KEY (action_id) REFERENCES iam_action (id),
    CONSTRAINT ck_iam_delta_operation CHECK (operation IN ('ADD', 'REMOVE', 'REPLACE_SCOPE')),
    CONSTRAINT ck_iam_delta_scopes CHECK (JSON_TYPE(scopes) = 'ARRAY' AND (operation <> 'REMOVE' OR JSON_LENGTH(scopes) = 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='历史租户角色定制差异';

-- 当前完整权限快照只能指向本角色。平台固定版本路径不依赖此指针。
ALTER TABLE iam_role_definition ADD CONSTRAINT fk_iam_role_current
    FOREIGN KEY (current_revision_id, id) REFERENCES iam_role_revision (id, role_id);
