-- 业务时间点统一 UTC；初始化连接也必须明确会话时区。
SET time_zone = '+00:00';

-- IAM 独立目标库：先执行 001、002。跨域关系由复合外键约束；权限上限仍由事务服务校验。
CREATE TABLE iam_delegation_grant (
    id BIGINT UNSIGNED NOT NULL COMMENT '受限委派及期限上限记录标识',
    domain VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '授权管理域：PLATFORM平台、TENANT租户',
    tenant_id BIGINT UNSIGNED NULL COMMENT '所属租户组织ID；NULL表示平台域或共享定义',
    tenant_key BIGINT UNSIGNED GENERATED ALWAYS AS (COALESCE(tenant_id, 0)) STORED COMMENT '自动生成的归属唯一键：tenant_id为空取0，否则取租户ID；用于复合唯一索引及外键',
    platform_administrator_id BIGINT UNSIGNED NULL COMMENT '被委派的平台管理员成员ID，平台域使用',
    tenant_administrator_id BIGINT UNSIGNED NULL COMMENT '被委派的租户管理员成员ID，租户域使用',
    valid_from DATETIME(6) NULL COMMENT '生效时间点，UTC；可空时表示不限制开始时间',
    valid_until DATETIME(6) NULL COMMENT '失效时间点，UTC；NULL表示不限制结束时间',
    assignment_duration_mode VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'LIMITED' COMMENT '单次授权期限模式：LIMITED限制时长、UNLIMITED无上限；后者仅平台域可用',
    max_assignment_duration_seconds BIGINT UNSIGNED NULL COMMENT '单次授权最大时长的秒部分；UNLIMITED时为NULL',
    max_assignment_duration_nanos INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '单次授权最大时长的纳秒部分，范围0至999999999',
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ACTIVE' COMMENT '委派状态：ACTIVE有效、REVOKED已撤销；时效和派生授权仍按业务校验',
    version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录创建时间点，UTC',
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_delegation_domain (domain, tenant_key, id),
    UNIQUE KEY uk_iam_delegation_tenant (tenant_id, id),
    KEY idx_iam_delegation_platform_admin (platform_administrator_id, status, valid_until),
    KEY idx_iam_delegation_tenant_admin (tenant_id, tenant_administrator_id, status, valid_until),
    CONSTRAINT fk_iam_delegation_platform_admin FOREIGN KEY (platform_administrator_id) REFERENCES iam_platform_member (id),
    CONSTRAINT fk_iam_delegation_tenant_admin FOREIGN KEY (tenant_id, tenant_administrator_id) REFERENCES iam_tenant_member (tenant_id, id),
    CONSTRAINT ck_iam_delegation_identity CHECK (
        (domain = 'PLATFORM' AND tenant_id IS NULL AND platform_administrator_id IS NOT NULL AND tenant_administrator_id IS NULL) OR
        (domain = 'TENANT' AND tenant_id IS NOT NULL AND tenant_administrator_id IS NOT NULL AND platform_administrator_id IS NULL)),
    CONSTRAINT ck_iam_delegation_status CHECK (status IN ('ACTIVE', 'REVOKED')),
    CONSTRAINT ck_iam_delegation_interval CHECK (valid_from IS NULL OR valid_until IS NULL OR valid_from < valid_until),
    CONSTRAINT ck_iam_delegation_duration CHECK (max_assignment_duration_nanos < 1000000000 AND
        ((assignment_duration_mode='LIMITED' AND max_assignment_duration_seconds IS NOT NULL
          AND (max_assignment_duration_seconds > 0 OR max_assignment_duration_nanos > 0))
         OR (assignment_duration_mode='UNLIMITED' AND domain='PLATFORM'
          AND max_assignment_duration_seconds IS NULL AND max_assignment_duration_nanos=0)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='受限委派及期限上限';

CREATE TABLE iam_delegation_role_revision (
    delegation_id BIGINT UNSIGNED NOT NULL COMMENT '委派标识；引用归属由本表外键及业务事务共同校验',
    revision_id BIGINT UNSIGNED NOT NULL COMMENT '角色固定版本标识；引用归属由本表外键及业务事务共同校验',
    revision_kind VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '被引用固定版本的角色种类，与版本ID共同验证引用合法性',
    PRIMARY KEY (delegation_id, revision_id),
    CONSTRAINT fk_iam_delegation_role_source FOREIGN KEY (delegation_id) REFERENCES iam_delegation_grant (id),
    CONSTRAINT fk_iam_delegation_role_revision FOREIGN KEY (revision_id, revision_kind) REFERENCES iam_role_revision (id, kind)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台委派允许的固定角色版本';

CREATE TABLE iam_delegation_recipient_member (
    delegation_id BIGINT UNSIGNED NOT NULL COMMENT '委派标识；引用归属由本表外键及业务事务共同校验',
    domain VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '授权管理域：PLATFORM平台、TENANT租户',
    tenant_id BIGINT UNSIGNED NULL COMMENT '所属租户组织ID；NULL表示平台域或共享定义',
    tenant_key BIGINT UNSIGNED GENERATED ALWAYS AS (COALESCE(tenant_id, 0)) STORED COMMENT '自动生成的归属唯一键：tenant_id为空取0，否则取租户ID；用于复合唯一索引及外键',
    platform_member_id BIGINT UNSIGNED NULL COMMENT '平台域接收成员ID；非平台成员主体时为空',
    tenant_member_id BIGINT UNSIGNED NULL COMMENT '租户域接收成员ID；非租户成员主体时为空',
    member_id BIGINT UNSIGNED GENERATED ALWAYS AS (COALESCE(platform_member_id, tenant_member_id)) STORED COMMENT '自动生成的接收成员键：取platform_member_id或tenant_member_id；与delegation_id唯一，禁止重复接收者',
    id BIGINT UNSIGNED NOT NULL COMMENT '委派接收成员关系记录标识；由IAM的Leaf发号器生成，不承载业务顺序',
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_delegation_recipient (delegation_id, member_id),
    CONSTRAINT fk_iam_recipient_delegation FOREIGN KEY (domain, tenant_key, delegation_id) REFERENCES iam_delegation_grant (domain, tenant_key, id),
    CONSTRAINT fk_iam_recipient_platform FOREIGN KEY (platform_member_id) REFERENCES iam_platform_member (id),
    CONSTRAINT fk_iam_recipient_tenant FOREIGN KEY (tenant_id, tenant_member_id) REFERENCES iam_tenant_member (tenant_id, id),
    CONSTRAINT ck_iam_recipient_identity CHECK (
        (domain = 'PLATFORM' AND tenant_id IS NULL AND platform_member_id IS NOT NULL AND tenant_member_id IS NULL) OR
        (domain = 'TENANT' AND tenant_id IS NOT NULL AND tenant_member_id IS NOT NULL AND platform_member_id IS NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='委派允许的接收成员';

CREATE TABLE iam_delegation_recipient_department (
    delegation_id BIGINT UNSIGNED NOT NULL COMMENT '委派标识；引用归属由本表外键及业务事务共同校验',
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    department_id BIGINT UNSIGNED NOT NULL COMMENT '部门标识；引用归属由本表外键及业务事务共同校验',
    include_descendants BOOLEAN NOT NULL DEFAULT FALSE COMMENT '选择该部门时是否包含下级部门成员：1包含、0仅直属',
    PRIMARY KEY (delegation_id, department_id),
    CONSTRAINT fk_iam_recipient_dept_delegation FOREIGN KEY (tenant_id, delegation_id) REFERENCES iam_delegation_grant (tenant_id, id),
    CONSTRAINT fk_iam_recipient_dept_department FOREIGN KEY (tenant_id, department_id) REFERENCES iam_department (tenant_id, id),
    CONSTRAINT ck_iam_recipient_descendants CHECK (include_descendants IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='委派允许的接收部门';

CREATE TABLE iam_delegation_action_ceiling (
    delegation_id BIGINT UNSIGNED NOT NULL COMMENT '委派标识；引用归属由本表外键及业务事务共同校验',
    action_id BIGINT UNSIGNED NOT NULL COMMENT '精确操作标识；引用归属由本表外键及业务事务共同校验',
    scopes JSON NOT NULL COMMENT '授权或上限的范围表达式JSON数组；具体空数组语义由对应求值器判定',
    scope_bindings JSON NOT NULL COMMENT '参数名到范围对象的绑定JSON对象；空对象表示无显式绑定',
    PRIMARY KEY (delegation_id, action_id),
    CONSTRAINT fk_iam_ceiling_delegation FOREIGN KEY (delegation_id) REFERENCES iam_delegation_grant (id),
    CONSTRAINT fk_iam_ceiling_action FOREIGN KEY (action_id) REFERENCES iam_action (id),
    CONSTRAINT ck_iam_ceiling_scopes CHECK (JSON_TYPE(scopes) = 'ARRAY'),
    CONSTRAINT ck_iam_ceiling_bindings CHECK (JSON_TYPE(scope_bindings) = 'OBJECT')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台委派逐操作范围上限';

CREATE TABLE iam_role_assignment (
    id BIGINT UNSIGNED NOT NULL COMMENT '成员或用户组角色分配记录标识',
    domain VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '授权管理域：PLATFORM平台、TENANT租户',
    tenant_id BIGINT UNSIGNED NULL COMMENT '所属租户组织ID；NULL表示平台域或共享定义',
    tenant_key BIGINT UNSIGNED GENERATED ALWAYS AS (COALESCE(tenant_id, 0)) STORED COMMENT '自动生成的归属唯一键：tenant_id为空取0，否则取租户ID；用于复合唯一索引及外键',
    subject_type VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '授权接收主体类别：MEMBER成员、GROUP用户组',
    platform_member_id BIGINT UNSIGNED NULL COMMENT '平台域接收成员ID；非平台成员主体时为空',
    platform_group_id BIGINT UNSIGNED NULL COMMENT '平台域接收用户组ID；非平台组主体时为空',
    tenant_member_id BIGINT UNSIGNED NULL COMMENT '租户域接收成员ID；非租户成员主体时为空',
    tenant_group_id BIGINT UNSIGNED NULL COMMENT '租户域接收用户组ID；非租户组主体时为空',
    role_id BIGINT UNSIGNED NULL COMMENT '租户分配引用的角色身份ID；平台固定版本路径为空；撤销历史记录可保留旧版本引用',
    role_kind VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT '被引用角色种类，与角色ID及归属共同验证合法性',
    role_tenant_key BIGINT UNSIGNED NULL COMMENT '角色归属键：共享角色为0，租户私有角色为对应租户ID，配合复合外键校验',
    revision_id BIGINT UNSIGNED NULL COMMENT '平台分配引用的固定版本ID；租户有效分配按role_id求当前快照',
    revision_kind VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT '被引用固定版本的角色种类，与版本ID共同验证引用合法性',
    scope_bindings JSON NOT NULL COMMENT '参数名到范围对象的绑定JSON对象；空对象表示无显式绑定',
    delegation_grant_id BIGINT UNSIGNED NULL COMMENT '来源委派标识；引用归属由本表外键及业务事务共同校验',
    valid_from DATETIME(6) NOT NULL COMMENT '生效时间点，UTC；可空时表示不限制开始时间',
    valid_until DATETIME(6) NULL COMMENT '失效时间点，UTC；NULL表示不限制结束时间',
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ACTIVE' COMMENT '分配状态：ACTIVE有效、REVOKED已撤销；有效期与主体状态还须独立校验',
    source VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '记录来源，取值及对应来源标识由所属业务契约解释；取值MANUAL,INITIALIZATION,MIGRATION',
    version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录创建时间点，UTC',
    PRIMARY KEY (id),
    KEY ix_iam_assignment_revision_status (revision_id,status,id),
    KEY idx_iam_assignment_platform_member (platform_member_id, status, valid_until),
    KEY idx_iam_assignment_platform_group (platform_group_id, status, valid_until),
    KEY idx_iam_assignment_tenant_member (tenant_id, tenant_member_id, status, valid_until),
    KEY idx_iam_assignment_tenant_group (tenant_id, tenant_group_id, status, valid_until),
    KEY idx_iam_assignment_delegation (delegation_grant_id, status),
    CONSTRAINT fk_iam_assignment_platform_member FOREIGN KEY (platform_member_id) REFERENCES iam_platform_member (id),
    CONSTRAINT fk_iam_assignment_platform_group FOREIGN KEY (platform_group_id) REFERENCES iam_platform_group (id),
    CONSTRAINT fk_iam_assignment_tenant_member FOREIGN KEY (tenant_id, tenant_member_id) REFERENCES iam_tenant_member (tenant_id, id),
    CONSTRAINT fk_iam_assignment_tenant_group FOREIGN KEY (tenant_id, tenant_group_id) REFERENCES iam_tenant_group (tenant_id, id),
    CONSTRAINT fk_iam_assignment_revision FOREIGN KEY (revision_id, revision_kind) REFERENCES iam_role_revision (id, kind),
    CONSTRAINT fk_iam_assignment_delegation FOREIGN KEY (domain, tenant_key, delegation_grant_id) REFERENCES iam_delegation_grant (domain, tenant_key, id),
    KEY idx_iam_assignment_current_role (role_id, status),
    CONSTRAINT fk_iam_assignment_current_role FOREIGN KEY (domain, role_tenant_key, role_id, role_kind)
        REFERENCES iam_role_definition (domain, tenant_key, id, kind),
    CONSTRAINT ck_iam_assignment_current_role CHECK (
        (domain='PLATFORM' AND role_id IS NULL AND revision_id IS NOT NULL AND revision_kind IS NOT NULL) OR
        (domain='TENANT' AND role_id IS NOT NULL AND role_kind IS NOT NULL AND role_tenant_key IS NOT NULL AND
          ((role_kind='SHARED' AND role_tenant_key=0) OR
           (role_kind='TENANT_CUSTOM' AND role_tenant_key=tenant_id))) OR
        (domain='TENANT' AND status='REVOKED' AND role_id IS NULL AND revision_id IS NOT NULL)),
    CONSTRAINT ck_iam_assignment_subject CHECK (
        (domain = 'PLATFORM' AND tenant_id IS NULL AND tenant_member_id IS NULL AND tenant_group_id IS NULL AND
            ((subject_type = 'MEMBER' AND platform_member_id IS NOT NULL AND platform_group_id IS NULL) OR
             (subject_type = 'GROUP' AND platform_group_id IS NOT NULL AND platform_member_id IS NULL))) OR
        (domain = 'TENANT' AND tenant_id IS NOT NULL AND platform_member_id IS NULL AND platform_group_id IS NULL AND
            ((subject_type = 'MEMBER' AND tenant_member_id IS NOT NULL AND tenant_group_id IS NULL) OR
             (subject_type = 'GROUP' AND tenant_group_id IS NOT NULL AND tenant_member_id IS NULL)))),
    CONSTRAINT ck_iam_assignment_status CHECK (status IN ('ACTIVE', 'REVOKED')),
    CONSTRAINT ck_iam_assignment_source CHECK (source IN ('MANUAL', 'INITIALIZATION', 'MIGRATION')),
    CONSTRAINT ck_iam_assignment_interval CHECK (valid_until IS NULL OR valid_from < valid_until),
    CONSTRAINT ck_iam_assignment_bindings CHECK (JSON_TYPE(scope_bindings) = 'OBJECT')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='成员或用户组角色分配';

CREATE TABLE iam_delegation_role (
    delegation_id BIGINT UNSIGNED NOT NULL COMMENT '委派标识；引用归属由本表外键及业务事务共同校验',
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    domain VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'TENANT' COMMENT '授权管理域：PLATFORM平台、TENANT租户',
    role_id BIGINT UNSIGNED NOT NULL COMMENT '角色标识；引用归属由本表外键及业务事务共同校验',
    role_kind VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '被引用角色种类，与角色ID及归属共同验证合法性',
    role_tenant_key BIGINT UNSIGNED NOT NULL COMMENT '角色归属键：共享角色为0，租户私有角色为对应租户ID，配合复合外键校验',
    PRIMARY KEY (delegation_id, role_id),
    CONSTRAINT fk_iam_live_delegation FOREIGN KEY (tenant_id, delegation_id) REFERENCES iam_delegation_grant (tenant_id, id),
    CONSTRAINT fk_iam_live_delegation_role FOREIGN KEY (domain, role_tenant_key, role_id, role_kind)
        REFERENCES iam_role_definition (domain, tenant_key, id, kind),
    CONSTRAINT ck_iam_live_delegation_role CHECK (domain='TENANT' AND
        ((role_kind='SHARED' AND role_tenant_key=0) OR (role_kind='TENANT_CUSTOM' AND role_tenant_key=tenant_id)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户委派允许的角色身份';

CREATE TABLE iam_delegation_resource_ceiling (
    delegation_id BIGINT UNSIGNED NOT NULL COMMENT '委派标识；引用归属由本表外键及业务事务共同校验',
    resource_id BIGINT UNSIGNED NOT NULL COMMENT '资源标识；引用归属由本表外键及业务事务共同校验',
    scopes JSON NOT NULL COMMENT '授权或上限的范围表达式JSON数组；具体空数组语义由对应求值器判定',
    scope_bindings JSON NOT NULL COMMENT '参数名到范围对象的绑定JSON对象；空对象表示无显式绑定',
    PRIMARY KEY (delegation_id, resource_id),
    CONSTRAINT fk_iam_live_resource_delegation FOREIGN KEY (delegation_id) REFERENCES iam_delegation_grant (id),
    CONSTRAINT fk_iam_live_resource FOREIGN KEY (resource_id) REFERENCES iam_resource (id),
    CONSTRAINT ck_iam_live_resource_scopes CHECK (JSON_TYPE(scopes)='ARRAY'),
    CONSTRAINT ck_iam_live_resource_bindings CHECK (JSON_TYPE(scope_bindings)='OBJECT')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户委派逐资源范围上限';
