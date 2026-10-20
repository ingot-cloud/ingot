-- 业务时间点统一 UTC；初始化连接也必须明确会话时区。
SET time_zone = '+00:00';

-- IAM 独立目标库：身份与组织结构。MySQL 8.0.16+；连接时区须为 UTC。
-- 仅在显式选择的全新目标库执行，不包含 USE、DROP、源库修改或默认数据。
-- 不是存量库升级脚本。组织创建事务须在提交前补齐有效 owner_member_id。

CREATE TABLE iam_account (
    id BIGINT UNSIGNED NOT NULL COMMENT '全局登录账号记录标识',
    username VARCHAR(64) COLLATE utf8mb4_bin NOT NULL COMMENT '全局唯一登录名，大小写按二进制排序规则区分',
    password_hash VARCHAR(300) NOT NULL COMMENT '不可逆密码哈希，禁止作为响应明文返回',
    phone VARCHAR(32) NULL COMMENT '账号登录手机号，可空；成员联系手机号独立保存',
    email VARCHAR(128) NULL COMMENT '账号邮箱，可空；成员联系邮箱独立保存',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用：1启用、0停用；不代表已授予操作权限',
    must_change_password BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否要求下次登录修改密码：1是、0否',
    password_changed_at DATETIME(6) NULL COMMENT '最近密码变更时间点，UTC；NULL表示尚无记录',
    last_login_at DATETIME(6) NULL COMMENT '最近登录时间点，UTC；NULL表示尚无记录',
    version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录创建时间点，UTC',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录最后更新时间点，UTC',
    deleted_at DATETIME(6) NULL COMMENT '软删除时间点，UTC；NULL表示未删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_account_username (username),
    KEY idx_iam_account_phone (phone),
    KEY idx_iam_account_email (email),
    CONSTRAINT ck_iam_account_id CHECK (id > 0),
    CONSTRAINT ck_iam_account_flags CHECK (enabled IN (0, 1) AND must_change_password IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='全局登录账号';

CREATE TABLE iam_platform_member (
    id BIGINT UNSIGNED NOT NULL COMMENT '平台成员资格与资料记录标识',
    account_id BIGINT UNSIGNED NOT NULL COMMENT '关联全局登录账号ID，成员资格及联系资料与账号独立',
    display_name VARCHAR(128) NOT NULL COMMENT '当前管理域内的成员显示姓名',
    avatar VARCHAR(512) NULL COMMENT '头像对象路径，仅存bucket/objectName；响应按权限签发临时链接',
    phone VARCHAR(32) NULL COMMENT '平台联系资料，不修改全局登录手机号',
    email VARCHAR(128) NULL COMMENT '平台联系资料，不修改全局账号邮箱',
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ACTIVE' COMMENT '平台成员资格：ACTIVE有效、SUSPENDED暂停、REMOVED移出；与账号启用状态独立',
    version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录创建时间点，UTC',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录最后更新时间点，UTC',
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_platform_member_account (account_id),
    KEY idx_iam_platform_member_status (status, id),
    CONSTRAINT fk_iam_platform_member_account FOREIGN KEY (account_id) REFERENCES iam_account (id),
    CONSTRAINT ck_iam_platform_member_id CHECK (id > 0),
    CONSTRAINT ck_iam_platform_member_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'REMOVED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台成员资格与资料';

CREATE TABLE iam_tenant (
    id BIGINT UNSIGNED NOT NULL COMMENT '租户组织记录标识',
    name VARCHAR(128) NOT NULL COMMENT '业务显示名称',
    avatar VARCHAR(512) NULL COMMENT '头像对象路径，仅存bucket/objectName；响应按权限签发临时链接',
    owner_member_id BIGINT UNSIGNED NULL COMMENT '创建事务内允许暂空；提交前必须是本租户有效成员',
    plan_id BIGINT UNSIGNED NULL COMMENT '最近一次明确提交的套餐ID；套餐目录变化不自动回写组织开通',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用：1启用、0停用；不代表已授予操作权限',
    version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录创建时间点，UTC',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录最后更新时间点，UTC',
    deleted_at DATETIME(6) NULL COMMENT '软删除时间点，UTC；NULL表示未删除',
    PRIMARY KEY (id),
    KEY idx_iam_tenant_owner (id, owner_member_id),
    CONSTRAINT ck_iam_tenant_id CHECK (id > 0),
    CONSTRAINT ck_iam_tenant_enabled CHECK (enabled IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户组织';

CREATE TABLE iam_tenant_member (
    id BIGINT UNSIGNED NOT NULL COMMENT '租户成员资格与通讯录资料记录标识',
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    account_id BIGINT UNSIGNED NOT NULL COMMENT '关联全局登录账号ID，成员资格及联系资料与账号独立',
    display_name VARCHAR(128) NOT NULL COMMENT '当前管理域内的成员显示姓名',
    avatar VARCHAR(512) NULL COMMENT '头像对象路径，仅存bucket/objectName；响应按权限签发临时链接',
    phone VARCHAR(32) NULL COMMENT '组织通讯录资料，不修改全局登录手机号',
    email VARCHAR(128) NULL COMMENT '组织通讯录资料，不修改全局登录邮箱',
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ACTIVE' COMMENT '当前租户成员资格：ACTIVE有效、SUSPENDED暂停、REMOVED移出；移出保留历史标识',
    version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录创建时间点，UTC',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录最后更新时间点，UTC',
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_tenant_member_account (tenant_id, account_id),
    UNIQUE KEY uk_iam_tenant_member_domain (tenant_id, id),
    KEY idx_iam_tenant_member_status (tenant_id, status, id),
    KEY idx_iam_tenant_member_account (account_id, status),
    CONSTRAINT fk_iam_tenant_member_account FOREIGN KEY (account_id) REFERENCES iam_account (id),
    CONSTRAINT fk_iam_tenant_member_tenant FOREIGN KEY (tenant_id) REFERENCES iam_tenant (id),
    CONSTRAINT ck_iam_tenant_member_id CHECK (id > 0),
    CONSTRAINT ck_iam_tenant_member_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'REMOVED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户成员资格与通讯录资料';

ALTER TABLE iam_tenant ADD CONSTRAINT fk_iam_tenant_owner
    FOREIGN KEY (id, owner_member_id) REFERENCES iam_tenant_member (tenant_id, id);

CREATE TABLE iam_department (
    id BIGINT UNSIGNED NOT NULL COMMENT '租户部门树记录标识',
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    parent_id BIGINT UNSIGNED NULL COMMENT '父节点ID；NULL表示根节点，必须属于相同归属域',
    name VARCHAR(128) NOT NULL COMMENT '业务显示名称',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '同级显示排序权重，由查询排序规则使用',
    version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录创建时间点，UTC',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录最后更新时间点，UTC',
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_department_domain (tenant_id, id),
    KEY idx_iam_department_parent (tenant_id, parent_id, id),
    CONSTRAINT fk_iam_department_tenant FOREIGN KEY (tenant_id) REFERENCES iam_tenant (id),
    CONSTRAINT fk_iam_department_parent FOREIGN KEY (tenant_id, parent_id) REFERENCES iam_department (tenant_id, id),
    CONSTRAINT ck_iam_department_id CHECK (id > 0),
    CONSTRAINT ck_iam_department_parent CHECK (parent_id IS NULL OR parent_id <> id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户部门树';

CREATE TABLE iam_member_department (
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    member_id BIGINT UNSIGNED NOT NULL COMMENT '成员标识；引用归属由本表外键及业务事务共同校验',
    department_id BIGINT UNSIGNED NOT NULL COMMENT '部门标识；引用归属由本表外键及业务事务共同校验',
    is_primary BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否为该成员的主部门：1是、0否；生成列唯一索引保证每成员至多一个',
    primary_member_id BIGINT UNSIGNED GENERATED ALWAYS AS (CASE WHEN is_primary = 1 THEN member_id ELSE NULL END) STORED COMMENT '自动约束键：主关系取member_id，否则NULL；与tenant_id唯一，保证成员至多一个主部门，非空集合业务规则由应用校验',
    PRIMARY KEY (tenant_id, member_id, department_id),
    UNIQUE KEY uk_iam_member_primary_department (tenant_id, primary_member_id),
    KEY idx_iam_department_members (tenant_id, department_id, member_id),
    CONSTRAINT fk_iam_member_department_member FOREIGN KEY (tenant_id, member_id) REFERENCES iam_tenant_member (tenant_id, id),
    CONSTRAINT fk_iam_member_department_department FOREIGN KEY (tenant_id, department_id) REFERENCES iam_department (tenant_id, id),
    CONSTRAINT ck_iam_member_department_primary CHECK (is_primary IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='成员任职部门关系';

CREATE TABLE iam_department_leader (
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    department_id BIGINT UNSIGNED NOT NULL COMMENT '部门标识；引用归属由本表外键及业务事务共同校验',
    member_id BIGINT UNSIGNED NOT NULL COMMENT '成员标识；引用归属由本表外键及业务事务共同校验',
    is_primary BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否为该部门的主负责人：1是、0否；非空集合恰有一个由业务事务校验',
    primary_department_id BIGINT UNSIGNED GENERATED ALWAYS AS (CASE WHEN is_primary = 1 THEN department_id ELSE NULL END) STORED COMMENT '自动约束键：主关系取department_id，否则NULL；与tenant_id唯一，保证部门至多一个主负责人，非空集合须恰有一个由应用校验',
    PRIMARY KEY (tenant_id, department_id, member_id),
    UNIQUE KEY uk_iam_department_primary_leader (tenant_id, primary_department_id),
    KEY idx_iam_member_led_departments (tenant_id, member_id, department_id),
    CONSTRAINT fk_iam_department_leader_department FOREIGN KEY (tenant_id, department_id) REFERENCES iam_department (tenant_id, id),
    CONSTRAINT fk_iam_department_leader_member FOREIGN KEY (tenant_id, member_id) REFERENCES iam_tenant_member (tenant_id, id),
    CONSTRAINT ck_iam_department_leader_primary CHECK (is_primary IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='部门负责人关系';

CREATE TABLE iam_platform_group (
    id BIGINT UNSIGNED NOT NULL COMMENT '平台静态用户组记录标识',
    name VARCHAR(128) NOT NULL COMMENT '业务显示名称',
    description VARCHAR(512) NULL COMMENT '业务说明，可空',
    version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录创建时间点，UTC',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录最后更新时间点，UTC',
    PRIMARY KEY (id),
    CONSTRAINT ck_iam_platform_group_id CHECK (id > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台静态用户组';

CREATE TABLE iam_platform_group_member (
    group_id BIGINT UNSIGNED NOT NULL COMMENT '用户组标识；引用归属由本表外键及业务事务共同校验',
    member_id BIGINT UNSIGNED NOT NULL COMMENT '成员标识；引用归属由本表外键及业务事务共同校验',
    PRIMARY KEY (group_id, member_id),
    KEY idx_iam_platform_member_groups (member_id, group_id),
    CONSTRAINT fk_iam_platform_group_entry_group FOREIGN KEY (group_id) REFERENCES iam_platform_group (id),
    CONSTRAINT fk_iam_platform_group_entry_member FOREIGN KEY (member_id) REFERENCES iam_platform_member (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台用户组显式成员';

CREATE TABLE iam_tenant_group (
    id BIGINT UNSIGNED NOT NULL COMMENT '租户静态用户组记录标识',
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    name VARCHAR(128) NOT NULL COMMENT '业务显示名称',
    description VARCHAR(512) NULL COMMENT '业务说明，可空',
    version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录创建时间点，UTC',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录最后更新时间点，UTC',
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_tenant_group_domain (tenant_id, id),
    CONSTRAINT fk_iam_tenant_group_tenant FOREIGN KEY (tenant_id) REFERENCES iam_tenant (id),
    CONSTRAINT ck_iam_tenant_group_id CHECK (id > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户静态用户组';

CREATE TABLE iam_tenant_group_member (
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    group_id BIGINT UNSIGNED NOT NULL COMMENT '用户组标识；引用归属由本表外键及业务事务共同校验',
    member_id BIGINT UNSIGNED NOT NULL COMMENT '成员标识；引用归属由本表外键及业务事务共同校验',
    PRIMARY KEY (tenant_id, group_id, member_id),
    KEY idx_iam_tenant_member_groups (tenant_id, member_id, group_id),
    CONSTRAINT fk_iam_tenant_group_entry_group FOREIGN KEY (tenant_id, group_id) REFERENCES iam_tenant_group (tenant_id, id),
    CONSTRAINT fk_iam_tenant_group_entry_member FOREIGN KEY (tenant_id, member_id) REFERENCES iam_tenant_member (tenant_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户用户组显式成员';

CREATE TABLE iam_tenant_group_department (
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    group_id BIGINT UNSIGNED NOT NULL COMMENT '用户组标识；引用归属由本表外键及业务事务共同校验',
    department_id BIGINT UNSIGNED NOT NULL COMMENT '部门标识；引用归属由本表外键及业务事务共同校验',
    include_descendants BOOLEAN NOT NULL DEFAULT FALSE COMMENT '选择该部门时是否包含下级部门成员：1包含、0仅直属',
    PRIMARY KEY (tenant_id, group_id, department_id),
    KEY idx_iam_tenant_department_groups (tenant_id, department_id, group_id),
    CONSTRAINT fk_iam_tenant_group_dept_group FOREIGN KEY (tenant_id, group_id) REFERENCES iam_tenant_group (tenant_id, id),
    CONSTRAINT fk_iam_tenant_group_dept_department FOREIGN KEY (tenant_id, department_id) REFERENCES iam_department (tenant_id, id),
    CONSTRAINT ck_iam_group_include_descendants CHECK (include_descendants IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户用户组部门来源';
