# IAM 数据字典

<!-- 自动生成：python3 tools/iam/generate_database.py；说明在权威 DDL 维护。 -->

manifest 管理 65 张表、525 个字段。

## 阅读约定

- 数据库字段定义列保留默认值、可空性及生成表达式；时间点统一 UTC。
- primary_member_id / primary_department_id 为自动约束键，不由接口填写；唯一索引保证至多一个主关系，业务完整性由事务服务校验。
- tenant_key 将空租户转为0，以便平台/共享归属参与唯一索引或复合外键；0不是实际租户。
- 角色 version、权限快照 revision、metadata_version 职责不同；并行生效模型按平台固定版本与租户当前角色分别解释。
- 历史辅助表列名不自动代表当前成员ID；第三方 app_id 与 IAM 应用目录ID按各字段说明区分。
- JSON 的数组/对象形态由列定义及 CHECK 约束，具体求值规则由对应模块负责；空集合不统一解释为全量或无权限。

主键与索引评估见 [逐表评估](DATABASE-REVIEW.md)。

## iam_account

全局登录账号

权威来源：[databases/iam/001_identity.sql](../../../databases/iam/001_identity.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `id` | ``id BIGINT UNSIGNED NOT NULL`` | 全局登录账号记录标识 |
| `username` | ``username VARCHAR(64) COLLATE utf8mb4_bin NOT NULL`` | 全局唯一登录名，大小写按二进制排序规则区分 |
| `password_hash` | ``password_hash VARCHAR(300) NOT NULL`` | 不可逆密码哈希，禁止作为响应明文返回 |
| `phone` | ``phone VARCHAR(32) NULL`` | 账号登录手机号，可空；成员联系手机号独立保存 |
| `email` | ``email VARCHAR(128) NULL`` | 账号邮箱，可空；成员联系邮箱独立保存 |
| `enabled` | ``enabled BOOLEAN NOT NULL DEFAULT TRUE`` | 是否启用：1启用、0停用；不代表已授予操作权限 |
| `must_change_password` | ``must_change_password BOOLEAN NOT NULL DEFAULT FALSE`` | 是否要求下次登录修改密码：1是、0否 |
| `password_changed_at` | ``password_changed_at DATETIME(6) NULL`` | 最近密码变更时间点，UTC；NULL表示尚无记录 |
| `last_login_at` | ``last_login_at DATETIME(6) NULL`` | 最近登录时间点，UTC；NULL表示尚无记录 |
| `version` | ``version BIGINT UNSIGNED NOT NULL DEFAULT 0`` | 记录乐观锁版本；变更时按所属业务事务递增 |
| `created_at` | ``created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)`` | 记录创建时间点，UTC |
| `updated_at` | ``updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)`` | 记录最后更新时间点，UTC |
| `deleted_at` | ``deleted_at DATETIME(6) NULL`` | 软删除时间点，UTC；NULL表示未删除 |

约束与索引：

```sql
PRIMARY KEY (id)
UNIQUE KEY uk_iam_account_username (username)
KEY idx_iam_account_phone (phone)
KEY idx_iam_account_email (email)
CONSTRAINT ck_iam_account_id CHECK (id > 0)
CONSTRAINT ck_iam_account_flags CHECK (enabled IN (0, 1) AND must_change_password IN (0, 1))
```

## iam_platform_member

平台成员资格与资料

权威来源：[databases/iam/001_identity.sql](../../../databases/iam/001_identity.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `id` | ``id BIGINT UNSIGNED NOT NULL`` | 平台成员资格与资料记录标识 |
| `account_id` | ``account_id BIGINT UNSIGNED NOT NULL`` | 关联全局登录账号ID，成员资格及联系资料与账号独立 |
| `display_name` | ``display_name VARCHAR(128) NOT NULL`` | 当前管理域内的成员显示姓名 |
| `avatar` | ``avatar VARCHAR(512) NULL`` | 头像对象路径，仅存bucket/objectName；响应按权限签发临时链接 |
| `phone` | ``phone VARCHAR(32) NULL`` | 平台联系资料，不修改全局登录手机号 |
| `email` | ``email VARCHAR(128) NULL`` | 平台联系资料，不修改全局账号邮箱 |
| `status` | ``status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ACTIVE'`` | 平台成员资格：ACTIVE有效、SUSPENDED暂停、REMOVED移出；与账号启用状态独立 |
| `version` | ``version BIGINT UNSIGNED NOT NULL DEFAULT 0`` | 记录乐观锁版本；变更时按所属业务事务递增 |
| `created_at` | ``created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)`` | 记录创建时间点，UTC |
| `updated_at` | ``updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)`` | 记录最后更新时间点，UTC |

约束与索引：

```sql
PRIMARY KEY (id)
UNIQUE KEY uk_iam_platform_member_account (account_id)
KEY idx_iam_platform_member_status (status, id)
CONSTRAINT fk_iam_platform_member_account FOREIGN KEY (account_id) REFERENCES iam_account (id)
CONSTRAINT ck_iam_platform_member_id CHECK (id > 0)
CONSTRAINT ck_iam_platform_member_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'REMOVED'))
```

## iam_tenant

租户组织

权威来源：[databases/iam/001_identity.sql](../../../databases/iam/001_identity.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `id` | ``id BIGINT UNSIGNED NOT NULL`` | 租户组织记录标识 |
| `name` | ``name VARCHAR(128) NOT NULL`` | 业务显示名称 |
| `avatar` | ``avatar VARCHAR(512) NULL`` | 头像对象路径，仅存bucket/objectName；响应按权限签发临时链接 |
| `owner_member_id` | ``owner_member_id BIGINT UNSIGNED NULL`` | 创建事务内允许暂空；提交前必须是本租户有效成员 |
| `plan_id` | ``plan_id BIGINT UNSIGNED NULL`` | 最近一次明确提交的套餐ID；套餐目录变化不自动回写组织开通 |
| `enabled` | ``enabled BOOLEAN NOT NULL DEFAULT TRUE`` | 是否启用：1启用、0停用；不代表已授予操作权限 |
| `version` | ``version BIGINT UNSIGNED NOT NULL DEFAULT 0`` | 记录乐观锁版本；变更时按所属业务事务递增 |
| `created_at` | ``created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)`` | 记录创建时间点，UTC |
| `updated_at` | ``updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)`` | 记录最后更新时间点，UTC |
| `deleted_at` | ``deleted_at DATETIME(6) NULL`` | 软删除时间点，UTC；NULL表示未删除 |

约束与索引：

```sql
PRIMARY KEY (id)
KEY idx_iam_tenant_owner (id, owner_member_id)
CONSTRAINT ck_iam_tenant_id CHECK (id > 0)
CONSTRAINT ck_iam_tenant_enabled CHECK (enabled IN (0, 1))
CONSTRAINT fk_iam_tenant_owner
    FOREIGN KEY (id, owner_member_id) REFERENCES iam_tenant_member (tenant_id, id)
```

## iam_tenant_member

租户成员资格与通讯录资料

权威来源：[databases/iam/001_identity.sql](../../../databases/iam/001_identity.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `id` | ``id BIGINT UNSIGNED NOT NULL`` | 租户成员资格与通讯录资料记录标识 |
| `tenant_id` | ``tenant_id BIGINT UNSIGNED NOT NULL`` | 所属租户组织ID |
| `account_id` | ``account_id BIGINT UNSIGNED NOT NULL`` | 关联全局登录账号ID，成员资格及联系资料与账号独立 |
| `display_name` | ``display_name VARCHAR(128) NOT NULL`` | 当前管理域内的成员显示姓名 |
| `avatar` | ``avatar VARCHAR(512) NULL`` | 头像对象路径，仅存bucket/objectName；响应按权限签发临时链接 |
| `phone` | ``phone VARCHAR(32) NULL`` | 组织通讯录资料，不修改全局登录手机号 |
| `email` | ``email VARCHAR(128) NULL`` | 组织通讯录资料，不修改全局登录邮箱 |
| `status` | ``status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ACTIVE'`` | 当前租户成员资格：ACTIVE有效、SUSPENDED暂停、REMOVED移出；移出保留历史标识 |
| `version` | ``version BIGINT UNSIGNED NOT NULL DEFAULT 0`` | 记录乐观锁版本；变更时按所属业务事务递增 |
| `created_at` | ``created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)`` | 记录创建时间点，UTC |
| `updated_at` | ``updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)`` | 记录最后更新时间点，UTC |

约束与索引：

```sql
PRIMARY KEY (id)
UNIQUE KEY uk_iam_tenant_member_account (tenant_id, account_id)
UNIQUE KEY uk_iam_tenant_member_domain (tenant_id, id)
KEY idx_iam_tenant_member_status (tenant_id, status, id)
KEY idx_iam_tenant_member_account (account_id, status)
CONSTRAINT fk_iam_tenant_member_account FOREIGN KEY (account_id) REFERENCES iam_account (id)
CONSTRAINT fk_iam_tenant_member_tenant FOREIGN KEY (tenant_id) REFERENCES iam_tenant (id)
CONSTRAINT ck_iam_tenant_member_id CHECK (id > 0)
CONSTRAINT ck_iam_tenant_member_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'REMOVED'))
```

## iam_department

租户部门树

权威来源：[databases/iam/001_identity.sql](../../../databases/iam/001_identity.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `id` | ``id BIGINT UNSIGNED NOT NULL`` | 租户部门树记录标识 |
| `tenant_id` | ``tenant_id BIGINT UNSIGNED NOT NULL`` | 所属租户组织ID |
| `parent_id` | ``parent_id BIGINT UNSIGNED NULL`` | 父节点ID；NULL表示根节点，必须属于相同归属域 |
| `name` | ``name VARCHAR(128) NOT NULL`` | 业务显示名称 |
| `sort_order` | ``sort_order INT NOT NULL DEFAULT 0`` | 同级显示排序权重，由查询排序规则使用 |
| `version` | ``version BIGINT UNSIGNED NOT NULL DEFAULT 0`` | 记录乐观锁版本；变更时按所属业务事务递增 |
| `created_at` | ``created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)`` | 记录创建时间点，UTC |
| `updated_at` | ``updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)`` | 记录最后更新时间点，UTC |

约束与索引：

```sql
PRIMARY KEY (id)
UNIQUE KEY uk_iam_department_domain (tenant_id, id)
KEY idx_iam_department_parent (tenant_id, parent_id, id)
CONSTRAINT fk_iam_department_tenant FOREIGN KEY (tenant_id) REFERENCES iam_tenant (id)
CONSTRAINT fk_iam_department_parent FOREIGN KEY (tenant_id, parent_id) REFERENCES iam_department (tenant_id, id)
CONSTRAINT ck_iam_department_id CHECK (id > 0)
CONSTRAINT ck_iam_department_parent CHECK (parent_id IS NULL OR parent_id <> id)
```

## iam_member_department

成员任职部门关系

权威来源：[databases/iam/001_identity.sql](../../../databases/iam/001_identity.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `tenant_id` | ``tenant_id BIGINT UNSIGNED NOT NULL`` | 所属租户组织ID |
| `member_id` | ``member_id BIGINT UNSIGNED NOT NULL`` | 成员标识；引用归属由本表外键及业务事务共同校验 |
| `department_id` | ``department_id BIGINT UNSIGNED NOT NULL`` | 部门标识；引用归属由本表外键及业务事务共同校验 |
| `is_primary` | ``is_primary BOOLEAN NOT NULL DEFAULT FALSE`` | 是否为该成员的主部门：1是、0否；生成列唯一索引保证每成员至多一个 |
| `primary_member_id` | ``primary_member_id BIGINT UNSIGNED GENERATED ALWAYS AS (CASE WHEN is_primary = 1 THEN member_id ELSE NULL END) STORED`` | 自动约束键：主关系取member_id，否则NULL；与tenant_id唯一，保证成员至多一个主部门，非空集合业务规则由应用校验 |

约束与索引：

```sql
PRIMARY KEY (tenant_id, member_id, department_id)
UNIQUE KEY uk_iam_member_primary_department (tenant_id, primary_member_id)
KEY idx_iam_department_members (tenant_id, department_id, member_id)
CONSTRAINT fk_iam_member_department_member FOREIGN KEY (tenant_id, member_id) REFERENCES iam_tenant_member (tenant_id, id)
CONSTRAINT fk_iam_member_department_department FOREIGN KEY (tenant_id, department_id) REFERENCES iam_department (tenant_id, id)
CONSTRAINT ck_iam_member_department_primary CHECK (is_primary IN (0, 1))
```

## iam_department_leader

部门负责人关系

权威来源：[databases/iam/001_identity.sql](../../../databases/iam/001_identity.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `tenant_id` | ``tenant_id BIGINT UNSIGNED NOT NULL`` | 所属租户组织ID |
| `department_id` | ``department_id BIGINT UNSIGNED NOT NULL`` | 部门标识；引用归属由本表外键及业务事务共同校验 |
| `member_id` | ``member_id BIGINT UNSIGNED NOT NULL`` | 成员标识；引用归属由本表外键及业务事务共同校验 |
| `is_primary` | ``is_primary BOOLEAN NOT NULL DEFAULT FALSE`` | 是否为该部门的主负责人：1是、0否；非空集合恰有一个由业务事务校验 |
| `primary_department_id` | ``primary_department_id BIGINT UNSIGNED GENERATED ALWAYS AS (CASE WHEN is_primary = 1 THEN department_id ELSE NULL END) STORED`` | 自动约束键：主关系取department_id，否则NULL；与tenant_id唯一，保证部门至多一个主负责人，非空集合须恰有一个由应用校验 |

约束与索引：

```sql
PRIMARY KEY (tenant_id, department_id, member_id)
UNIQUE KEY uk_iam_department_primary_leader (tenant_id, primary_department_id)
KEY idx_iam_member_led_departments (tenant_id, member_id, department_id)
CONSTRAINT fk_iam_department_leader_department FOREIGN KEY (tenant_id, department_id) REFERENCES iam_department (tenant_id, id)
CONSTRAINT fk_iam_department_leader_member FOREIGN KEY (tenant_id, member_id) REFERENCES iam_tenant_member (tenant_id, id)
CONSTRAINT ck_iam_department_leader_primary CHECK (is_primary IN (0, 1))
```

## iam_platform_group

平台静态用户组

权威来源：[databases/iam/001_identity.sql](../../../databases/iam/001_identity.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `id` | ``id BIGINT UNSIGNED NOT NULL`` | 平台静态用户组记录标识 |
| `name` | ``name VARCHAR(128) NOT NULL`` | 业务显示名称 |
| `description` | ``description VARCHAR(512) NULL`` | 业务说明，可空 |
| `version` | ``version BIGINT UNSIGNED NOT NULL DEFAULT 0`` | 记录乐观锁版本；变更时按所属业务事务递增 |
| `created_at` | ``created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)`` | 记录创建时间点，UTC |
| `updated_at` | ``updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)`` | 记录最后更新时间点，UTC |

约束与索引：

```sql
PRIMARY KEY (id)
CONSTRAINT ck_iam_platform_group_id CHECK (id > 0)
```

## iam_platform_group_member

平台用户组显式成员

权威来源：[databases/iam/001_identity.sql](../../../databases/iam/001_identity.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `group_id` | ``group_id BIGINT UNSIGNED NOT NULL`` | 用户组标识；引用归属由本表外键及业务事务共同校验 |
| `member_id` | ``member_id BIGINT UNSIGNED NOT NULL`` | 成员标识；引用归属由本表外键及业务事务共同校验 |

约束与索引：

```sql
PRIMARY KEY (group_id, member_id)
KEY idx_iam_platform_member_groups (member_id, group_id)
CONSTRAINT fk_iam_platform_group_entry_group FOREIGN KEY (group_id) REFERENCES iam_platform_group (id)
CONSTRAINT fk_iam_platform_group_entry_member FOREIGN KEY (member_id) REFERENCES iam_platform_member (id)
```

## iam_tenant_group

租户静态用户组

权威来源：[databases/iam/001_identity.sql](../../../databases/iam/001_identity.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `id` | ``id BIGINT UNSIGNED NOT NULL`` | 租户静态用户组记录标识 |
| `tenant_id` | ``tenant_id BIGINT UNSIGNED NOT NULL`` | 所属租户组织ID |
| `name` | ``name VARCHAR(128) NOT NULL`` | 业务显示名称 |
| `description` | ``description VARCHAR(512) NULL`` | 业务说明，可空 |
| `version` | ``version BIGINT UNSIGNED NOT NULL DEFAULT 0`` | 记录乐观锁版本；变更时按所属业务事务递增 |
| `created_at` | ``created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)`` | 记录创建时间点，UTC |
| `updated_at` | ``updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)`` | 记录最后更新时间点，UTC |

约束与索引：

```sql
PRIMARY KEY (id)
UNIQUE KEY uk_iam_tenant_group_domain (tenant_id, id)
CONSTRAINT fk_iam_tenant_group_tenant FOREIGN KEY (tenant_id) REFERENCES iam_tenant (id)
CONSTRAINT ck_iam_tenant_group_id CHECK (id > 0)
```

## iam_tenant_group_member

租户用户组显式成员

权威来源：[databases/iam/001_identity.sql](../../../databases/iam/001_identity.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `tenant_id` | ``tenant_id BIGINT UNSIGNED NOT NULL`` | 所属租户组织ID |
| `group_id` | ``group_id BIGINT UNSIGNED NOT NULL`` | 用户组标识；引用归属由本表外键及业务事务共同校验 |
| `member_id` | ``member_id BIGINT UNSIGNED NOT NULL`` | 成员标识；引用归属由本表外键及业务事务共同校验 |

约束与索引：

```sql
PRIMARY KEY (tenant_id, group_id, member_id)
KEY idx_iam_tenant_member_groups (tenant_id, member_id, group_id)
CONSTRAINT fk_iam_tenant_group_entry_group FOREIGN KEY (tenant_id, group_id) REFERENCES iam_tenant_group (tenant_id, id)
CONSTRAINT fk_iam_tenant_group_entry_member FOREIGN KEY (tenant_id, member_id) REFERENCES iam_tenant_member (tenant_id, id)
```

## iam_tenant_group_department

租户用户组部门来源

权威来源：[databases/iam/001_identity.sql](../../../databases/iam/001_identity.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `tenant_id` | ``tenant_id BIGINT UNSIGNED NOT NULL`` | 所属租户组织ID |
| `group_id` | ``group_id BIGINT UNSIGNED NOT NULL`` | 用户组标识；引用归属由本表外键及业务事务共同校验 |
| `department_id` | ``department_id BIGINT UNSIGNED NOT NULL`` | 部门标识；引用归属由本表外键及业务事务共同校验 |
| `include_descendants` | ``include_descendants BOOLEAN NOT NULL DEFAULT FALSE`` | 选择该部门时是否包含下级部门成员：1包含、0仅直属 |

约束与索引：

```sql
PRIMARY KEY (tenant_id, group_id, department_id)
KEY idx_iam_tenant_department_groups (tenant_id, department_id, group_id)
CONSTRAINT fk_iam_tenant_group_dept_group FOREIGN KEY (tenant_id, group_id) REFERENCES iam_tenant_group (tenant_id, id)
CONSTRAINT fk_iam_tenant_group_dept_department FOREIGN KEY (tenant_id, department_id) REFERENCES iam_department (tenant_id, id)
CONSTRAINT ck_iam_group_include_descendants CHECK (include_descendants IN (0, 1))
```

## iam_application

应用目录

权威来源：[databases/iam/002_catalog_role.sql](../../../databases/iam/002_catalog_role.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `id` | ``id BIGINT UNSIGNED NOT NULL`` | 应用目录记录标识 |
| `code` | ``code VARCHAR(64) COLLATE utf8mb4_bin NOT NULL`` | 业务稳定编码，唯一性由本表索引限定 |
| `domain` | ``domain VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL`` | 授权管理域：PLATFORM平台、TENANT租户 |
| `name` | ``name VARCHAR(128) NOT NULL`` | 业务显示名称 |
| `description` | ``description VARCHAR(512) NULL`` | 业务说明，可空 |
| `icon` | ``icon VARCHAR(512) NULL`` | 界面图标标识或配置值，按所属应用菜单契约解释 |
| `sort_order` | ``sort_order INT NOT NULL DEFAULT 0`` | 同级显示排序权重，由查询排序规则使用 |
| `baseline` | ``baseline BOOLEAN NOT NULL DEFAULT FALSE`` | 是否为租户基础治理应用：1是、0否；基础应用开通不可关闭 |
| `enabled` | ``enabled BOOLEAN NOT NULL DEFAULT TRUE`` | 是否启用：1启用、0停用；不代表已授予操作权限 |
| `version` | ``version BIGINT UNSIGNED NOT NULL DEFAULT 0`` | 记录乐观锁版本；变更时按所属业务事务递增 |

约束与索引：

```sql
PRIMARY KEY (id)
UNIQUE KEY uk_iam_application_code (code)
CONSTRAINT ck_iam_application_domain CHECK (domain IN ('PLATFORM', 'TENANT'))
CONSTRAINT ck_iam_application_baseline CHECK (baseline IN (0, 1))
CONSTRAINT ck_iam_application_baseline_domain CHECK (baseline = FALSE OR domain = 'TENANT')
CONSTRAINT ck_iam_application_enabled CHECK (enabled IN (0, 1))
```

## iam_resource

应用资源及范围字段能力

权威来源：[databases/iam/002_catalog_role.sql](../../../databases/iam/002_catalog_role.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `id` | ``id BIGINT UNSIGNED NOT NULL`` | 应用资源及范围字段能力记录标识 |
| `application_id` | ``application_id BIGINT UNSIGNED NOT NULL`` | 应用标识；引用归属由本表外键及业务事务共同校验 |
| `code` | ``code VARCHAR(64) COLLATE utf8mb4_bin NOT NULL`` | 业务稳定编码，唯一性由本表索引限定 |
| `name` | ``name VARCHAR(128) NOT NULL`` | 业务显示名称 |
| `scope_capabilities` | ``scope_capabilities JSON NOT NULL`` | 资源支持的范围能力JSON数组，作为授权范围校验上限 |
| `field_capabilities` | ``field_capabilities JSON NOT NULL`` | 资源字段能力JSON数组，限制可见性、编辑及筛选等可声明能力 |
| `enabled` | ``enabled BOOLEAN NOT NULL DEFAULT TRUE`` | 是否启用：1启用、0停用；不代表已授予操作权限 |
| `version` | ``version BIGINT UNSIGNED NOT NULL DEFAULT 0`` | 记录乐观锁版本；变更时按所属业务事务递增 |

约束与索引：

```sql
PRIMARY KEY (id)
UNIQUE KEY uk_iam_resource_code (application_id, code)
UNIQUE KEY uk_iam_resource_application (application_id, id)
CONSTRAINT fk_iam_resource_application FOREIGN KEY (application_id) REFERENCES iam_application (id)
CONSTRAINT ck_iam_resource_scopes CHECK (JSON_TYPE(scope_capabilities) = 'ARRAY')
CONSTRAINT ck_iam_resource_fields CHECK (JSON_TYPE(field_capabilities) = 'ARRAY')
CONSTRAINT ck_iam_resource_enabled CHECK (enabled IN (0, 1))
```

## iam_action

资源精确操作目录

权威来源：[databases/iam/002_catalog_role.sql](../../../databases/iam/002_catalog_role.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `id` | ``id BIGINT UNSIGNED NOT NULL`` | 资源精确操作目录记录标识 |
| `application_id` | ``application_id BIGINT UNSIGNED NOT NULL`` | 应用标识；引用归属由本表外键及业务事务共同校验 |
| `resource_id` | ``resource_id BIGINT UNSIGNED NOT NULL`` | 资源标识；引用归属由本表外键及业务事务共同校验 |
| `code` | ``code VARCHAR(192) COLLATE utf8mb4_bin NOT NULL`` | 全局唯一精确操作码，禁止通配符，例如iam-tenant:member:read |
| `name` | ``name VARCHAR(128) NOT NULL`` | 业务显示名称 |
| `enabled` | ``enabled BOOLEAN NOT NULL DEFAULT TRUE`` | 是否启用：1启用、0停用；不代表已授予操作权限 |
| `version` | ``version BIGINT UNSIGNED NOT NULL DEFAULT 0`` | 记录乐观锁版本；变更时按所属业务事务递增 |

约束与索引：

```sql
PRIMARY KEY (id)
UNIQUE KEY uk_iam_action_code (code)
UNIQUE KEY uk_iam_action_application (application_id, id)
KEY idx_iam_action_resource (application_id, resource_id, id)
CONSTRAINT fk_iam_action_resource FOREIGN KEY (application_id, resource_id) REFERENCES iam_resource (application_id, id)
CONSTRAINT ck_iam_action_exact CHECK (LOCATE('*', code) = 0 AND CHAR_LENGTH(code) > 0)
CONSTRAINT ck_iam_action_enabled CHECK (enabled IN (0, 1))
```

## iam_menu

应用菜单与前端路由

权威来源：[databases/iam/002_catalog_role.sql](../../../databases/iam/002_catalog_role.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `id` | ``id BIGINT UNSIGNED NOT NULL`` | 应用菜单与前端路由记录标识 |
| `application_id` | ``application_id BIGINT UNSIGNED NOT NULL`` | 应用标识；引用归属由本表外键及业务事务共同校验 |
| `parent_id` | ``parent_id BIGINT UNSIGNED NULL`` | 父节点ID；NULL表示根节点，必须属于相同归属域 |
| `name` | ``name VARCHAR(128) NOT NULL`` | 业务显示名称 |
| `path` | ``path VARCHAR(512) NULL`` | 前端菜单路由路径，可空 |
| `view_path` | ``view_path VARCHAR(256) NULL`` | 前端视图注册键；目录布局使用约定布局键 |
| `route_name` | ``route_name VARCHAR(128) NULL`` | 前端路由稳定名称，可空 |
| `icon` | ``icon VARCHAR(512) NULL`` | 界面图标标识或配置值，按所属应用菜单契约解释 |
| `kind` | ``kind VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL`` | 菜单类别：DIRECTORY目录、PAGE页面 |
| `match_mode` | ``match_mode VARCHAR(8) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ANY'`` | 菜单操作匹配方式：ANY任一、ALL全部；取值ANY,ALL |
| `access_mode` | ``access_mode VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ACTION'`` | 菜单准入模式：OPEN开放、ACTION按绑定操作判断；取值OPEN,ACTION |
| `sort_order` | ``sort_order INT NOT NULL DEFAULT 0`` | 同级显示排序权重，由查询排序规则使用 |
| `hidden` | ``hidden BOOLEAN NOT NULL DEFAULT FALSE`` | 是否在菜单中隐藏：1隐藏、0展示；不授予访问权限 |
| `is_cache` | ``is_cache BOOLEAN NOT NULL DEFAULT FALSE`` | 是否启用前端页面缓存：1是、0否 |
| `props` | ``props BOOLEAN NOT NULL DEFAULT FALSE`` | 是否将路由参数作为页面属性传入：1是、0否 |
| `route_params` | ``route_params JSON NULL`` | 路由参数JSON，可空 |
| `enabled` | ``enabled BOOLEAN NOT NULL DEFAULT TRUE`` | 是否启用：1启用、0停用；不代表已授予操作权限 |
| `version` | ``version BIGINT UNSIGNED NOT NULL DEFAULT 0`` | 记录乐观锁版本；变更时按所属业务事务递增 |

约束与索引：

```sql
PRIMARY KEY (id)
UNIQUE KEY uk_iam_menu_application (application_id, id)
KEY idx_iam_menu_parent (application_id, parent_id, sort_order, id)
CONSTRAINT fk_iam_menu_application FOREIGN KEY (application_id) REFERENCES iam_application (id)
CONSTRAINT fk_iam_menu_parent FOREIGN KEY (application_id, parent_id) REFERENCES iam_menu (application_id, id)
CONSTRAINT ck_iam_menu_kind CHECK (kind IN ('DIRECTORY', 'PAGE'))
CONSTRAINT ck_iam_menu_match CHECK (match_mode IN ('ANY', 'ALL'))
CONSTRAINT ck_iam_menu_access CHECK (access_mode IN ('OPEN', 'ACTION'))
CONSTRAINT ck_iam_menu_parent CHECK (parent_id IS NULL OR parent_id <> id)
CONSTRAINT ck_iam_menu_enabled CHECK (enabled IN (0, 1))
```

## iam_menu_action

菜单操作绑定

权威来源：[databases/iam/002_catalog_role.sql](../../../databases/iam/002_catalog_role.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `application_id` | ``application_id BIGINT UNSIGNED NOT NULL`` | 应用标识；引用归属由本表外键及业务事务共同校验 |
| `menu_id` | ``menu_id BIGINT UNSIGNED NOT NULL`` | 菜单标识；引用归属由本表外键及业务事务共同校验 |
| `action_id` | ``action_id BIGINT UNSIGNED NOT NULL`` | 精确操作标识；引用归属由本表外键及业务事务共同校验 |

约束与索引：

```sql
PRIMARY KEY (application_id, menu_id, action_id)
KEY idx_iam_action_menus (application_id, action_id, menu_id)
CONSTRAINT fk_iam_menu_action_menu FOREIGN KEY (application_id, menu_id) REFERENCES iam_menu (application_id, id)
CONSTRAINT fk_iam_menu_action_action FOREIGN KEY (application_id, action_id) REFERENCES iam_action (application_id, id)
```

## iam_tenant_app_entitlement

租户应用开通

权威来源：[databases/iam/002_catalog_role.sql](../../../databases/iam/002_catalog_role.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `id` | ``id BIGINT UNSIGNED NOT NULL`` | 租户应用开通记录标识 |
| `tenant_id` | ``tenant_id BIGINT UNSIGNED NOT NULL`` | 所属租户组织ID |
| `application_id` | ``application_id BIGINT UNSIGNED NOT NULL`` | 应用标识；引用归属由本表外键及业务事务共同校验 |
| `enabled` | ``enabled BOOLEAN NOT NULL`` | 是否启用：1启用、0停用；不代表已授予操作权限 |
| `source` | ``source VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL`` | 记录来源，取值及对应来源标识由所属业务契约解释；取值INITIALIZATION,MANUAL,PLAN,MIGRATION |
| `source_id` | ``source_id BIGINT UNSIGNED NULL`` | 开通来源对象ID；PLAN来源记录套餐ID，其他来源按业务规则可空 |
| `valid_from` | ``valid_from DATETIME(6) NULL`` | 生效时间点，UTC；可空时表示不限制开始时间 |
| `valid_until` | ``valid_until DATETIME(6) NULL`` | 失效时间点，UTC；NULL表示不限制结束时间 |
| `version` | ``version BIGINT UNSIGNED NOT NULL DEFAULT 0`` | 记录乐观锁版本；变更时按所属业务事务递增 |

约束与索引：

```sql
PRIMARY KEY (id)
UNIQUE KEY uk_iam_entitlement (tenant_id, application_id)
CONSTRAINT fk_iam_entitlement_tenant FOREIGN KEY (tenant_id) REFERENCES iam_tenant (id)
CONSTRAINT fk_iam_entitlement_app FOREIGN KEY (application_id) REFERENCES iam_application (id)
CONSTRAINT ck_iam_entitlement_source CHECK (source IN ('INITIALIZATION', 'MANUAL', 'PLAN', 'MIGRATION'))
CONSTRAINT ck_iam_entitlement_interval CHECK (valid_from IS NULL OR valid_until IS NULL OR valid_from < valid_until)
CONSTRAINT ck_iam_entitlement_enabled CHECK (enabled IN (0, 1))
```

## iam_app_audience

租户应用可用人群配置

权威来源：[databases/iam/002_catalog_role.sql](../../../databases/iam/002_catalog_role.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `tenant_id` | ``tenant_id BIGINT UNSIGNED NOT NULL`` | 所属租户组织ID |
| `application_id` | ``application_id BIGINT UNSIGNED NOT NULL`` | 应用标识；引用归属由本表外键及业务事务共同校验 |
| `enabled` | ``enabled BOOLEAN NOT NULL DEFAULT TRUE`` | 是否启用：1启用、0停用；不代表已授予操作权限 |
| `audience_kind` | ``audience_kind VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL`` | 应用可用人群：ALL全部成员、SELECTED指定来源；开通不等于操作授权；取值ALL,SELECTED |
| `version` | ``version BIGINT UNSIGNED NOT NULL DEFAULT 0`` | 记录乐观锁版本；变更时按所属业务事务递增 |

约束与索引：

```sql
PRIMARY KEY (tenant_id, application_id)
CONSTRAINT fk_iam_audience_entitlement FOREIGN KEY (tenant_id, application_id) REFERENCES iam_tenant_app_entitlement (tenant_id, application_id)
CONSTRAINT ck_iam_audience_kind CHECK (audience_kind IN ('ALL', 'SELECTED'))
CONSTRAINT ck_iam_audience_enabled CHECK (enabled IN (0, 1))
```

## iam_audience_member

应用人群显式成员

权威来源：[databases/iam/002_catalog_role.sql](../../../databases/iam/002_catalog_role.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `tenant_id` | ``tenant_id BIGINT UNSIGNED NOT NULL`` | 所属租户组织ID |
| `application_id` | ``application_id BIGINT UNSIGNED NOT NULL`` | 应用标识；引用归属由本表外键及业务事务共同校验 |
| `member_id` | ``member_id BIGINT UNSIGNED NOT NULL`` | 成员标识；引用归属由本表外键及业务事务共同校验 |

约束与索引：

```sql
PRIMARY KEY (tenant_id, application_id, member_id)
CONSTRAINT fk_iam_audience_member_config FOREIGN KEY (tenant_id, application_id) REFERENCES iam_app_audience (tenant_id, application_id)
CONSTRAINT fk_iam_audience_member FOREIGN KEY (tenant_id, member_id) REFERENCES iam_tenant_member (tenant_id, id)
```

## iam_audience_department

应用人群部门来源

权威来源：[databases/iam/002_catalog_role.sql](../../../databases/iam/002_catalog_role.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `tenant_id` | ``tenant_id BIGINT UNSIGNED NOT NULL`` | 所属租户组织ID |
| `application_id` | ``application_id BIGINT UNSIGNED NOT NULL`` | 应用标识；引用归属由本表外键及业务事务共同校验 |
| `department_id` | ``department_id BIGINT UNSIGNED NOT NULL`` | 部门标识；引用归属由本表外键及业务事务共同校验 |
| `include_descendants` | ``include_descendants BOOLEAN NOT NULL DEFAULT FALSE`` | 选择该部门时是否包含下级部门成员：1包含、0仅直属 |

约束与索引：

```sql
PRIMARY KEY (tenant_id, application_id, department_id)
CONSTRAINT fk_iam_audience_dept_config FOREIGN KEY (tenant_id, application_id) REFERENCES iam_app_audience (tenant_id, application_id)
CONSTRAINT fk_iam_audience_dept FOREIGN KEY (tenant_id, department_id) REFERENCES iam_department (tenant_id, id)
CONSTRAINT ck_iam_audience_descendants CHECK (include_descendants IN (0, 1))
```

## iam_audience_group

应用人群用户组来源

权威来源：[databases/iam/002_catalog_role.sql](../../../databases/iam/002_catalog_role.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `tenant_id` | ``tenant_id BIGINT UNSIGNED NOT NULL`` | 所属租户组织ID |
| `application_id` | ``application_id BIGINT UNSIGNED NOT NULL`` | 应用标识；引用归属由本表外键及业务事务共同校验 |
| `group_id` | ``group_id BIGINT UNSIGNED NOT NULL`` | 用户组标识；引用归属由本表外键及业务事务共同校验 |

约束与索引：

```sql
PRIMARY KEY (tenant_id, application_id, group_id)
CONSTRAINT fk_iam_audience_group_config FOREIGN KEY (tenant_id, application_id) REFERENCES iam_app_audience (tenant_id, application_id)
CONSTRAINT fk_iam_audience_group FOREIGN KEY (tenant_id, group_id) REFERENCES iam_tenant_group (tenant_id, id)
```

## iam_plan

套餐目录

权威来源：[databases/iam/002_catalog_role.sql](../../../databases/iam/002_catalog_role.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `id` | ``id BIGINT UNSIGNED NOT NULL`` | 套餐目录记录标识 |
| `name` | ``name VARCHAR(128) NOT NULL`` | 业务显示名称 |
| `description` | ``description VARCHAR(512) NULL`` | 业务说明，可空 |
| `enabled` | ``enabled BOOLEAN NOT NULL DEFAULT TRUE`` | 是否启用：1启用、0停用；不代表已授予操作权限 |
| `version` | ``version BIGINT UNSIGNED NOT NULL DEFAULT 0`` | 记录乐观锁版本；变更时按所属业务事务递增 |

约束与索引：

```sql
PRIMARY KEY (id)
CONSTRAINT ck_iam_plan_enabled CHECK (enabled IN (0, 1))
```

## iam_plan_application

套餐包含的应用

权威来源：[databases/iam/002_catalog_role.sql](../../../databases/iam/002_catalog_role.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `plan_id` | ``plan_id BIGINT UNSIGNED NOT NULL`` | 套餐标识；引用归属由本表外键及业务事务共同校验 |
| `application_id` | ``application_id BIGINT UNSIGNED NOT NULL`` | 应用标识；引用归属由本表外键及业务事务共同校验 |

约束与索引：

```sql
PRIMARY KEY (plan_id, application_id)
CONSTRAINT fk_iam_plan_application_plan FOREIGN KEY (plan_id) REFERENCES iam_plan (id)
CONSTRAINT fk_iam_plan_application_app FOREIGN KEY (application_id) REFERENCES iam_application (id)
```

## iam_role_definition

角色身份与当前快照指针

权威来源：[databases/iam/002_catalog_role.sql](../../../databases/iam/002_catalog_role.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `id` | ``id BIGINT UNSIGNED NOT NULL`` | 角色身份与当前快照指针记录标识 |
| `domain` | ``domain VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL`` | 授权管理域：PLATFORM平台、TENANT租户 |
| `tenant_id` | ``tenant_id BIGINT UNSIGNED NULL`` | 所属租户组织ID；NULL表示平台域或共享定义 |
| `tenant_key` | ``tenant_key BIGINT UNSIGNED GENERATED ALWAYS AS (COALESCE(tenant_id, 0)) STORED`` | 自动生成的归属唯一键：tenant_id为空取0，否则取租户ID；用于复合唯一索引及外键 |
| `kind` | ``kind VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL`` | 角色类别：SYSTEM系统、SHARED共享、PLATFORM_CUSTOM平台自有、TENANT_CUSTOM租户私有 |
| `code` | ``code VARCHAR(128) COLLATE utf8mb4_bin NOT NULL`` | 业务稳定编码，唯一性由本表索引限定 |
| `name` | ``name VARCHAR(128) NOT NULL`` | 业务显示名称 |
| `description` | ``description VARCHAR(512) NULL`` | 业务说明，可空 |
| `group_name` | ``group_name VARCHAR(128) NULL`` | 角色展示分组名称，可空 |
| `enabled` | ``enabled BOOLEAN NOT NULL DEFAULT TRUE`` | 是否启用：1启用、0停用；不代表已授予操作权限 |
| `version` | ``version BIGINT UNSIGNED NOT NULL DEFAULT 0`` | 记录乐观锁版本；变更时按所属业务事务递增 |
| `current_revision_id` | ``current_revision_id BIGINT UNSIGNED NULL`` | 共享或租户私有角色当前完整权限快照ID；平台固定版本路径不依赖此指针 |

约束与索引：

```sql
PRIMARY KEY (id)
UNIQUE KEY uk_iam_role_code (domain, tenant_key, code)
UNIQUE KEY uk_iam_role_kind (id, kind)
UNIQUE KEY uk_iam_role_owner (domain, tenant_key, id, kind)
CONSTRAINT fk_iam_role_tenant FOREIGN KEY (tenant_id) REFERENCES iam_tenant (id)
CONSTRAINT ck_iam_role_domain CHECK (domain IN ('PLATFORM', 'TENANT'))
CONSTRAINT ck_iam_role_kind CHECK (
        (kind = 'SYSTEM' AND tenant_id IS NULL) OR
        (kind = 'SHARED' AND domain = 'TENANT' AND tenant_id IS NULL) OR
        (kind = 'PLATFORM_CUSTOM' AND domain = 'PLATFORM' AND tenant_id IS NULL) OR
        (kind = 'TENANT_CUSTOM' AND domain = 'TENANT' AND tenant_id IS NOT NULL))
CONSTRAINT ck_iam_role_enabled CHECK (enabled IN (0, 1))
CONSTRAINT fk_iam_role_current
    FOREIGN KEY (current_revision_id, id) REFERENCES iam_role_revision (id, role_id)
```

## iam_role_revision

角色不可变权限快照与可编辑版本元数据

权威来源：[databases/iam/002_catalog_role.sql](../../../databases/iam/002_catalog_role.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `id` | ``id BIGINT UNSIGNED NOT NULL`` | 角色不可变权限快照与可编辑版本元数据记录标识 |
| `role_id` | ``role_id BIGINT UNSIGNED NOT NULL`` | 角色标识；引用归属由本表外键及业务事务共同校验 |
| `kind` | ``kind VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL`` | 权限快照所属角色类别，与role_id共同引用角色；快照权限内容不可变 |
| `revision` | ``revision BIGINT UNSIGNED NOT NULL`` | 不可变快照的发布序号，从1开始；与记录乐观锁版本不同 |
| `base_revision_id` | ``base_revision_id BIGINT UNSIGNED NULL`` | 历史定制角色引用的共享基础版本ID；NULL表示无基础版本 |
| `base_kind` | ``base_kind VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin         GENERATED ALWAYS AS (CASE WHEN base_revision_id IS NULL THEN NULL ELSE 'SHARED' END) STORED`` | 自动生成的基础版本种类：有base_revision_id时为SHARED，否则NULL，用于复合外键 |
| `metadata_overrides` | ``metadata_overrides JSON NOT NULL`` | 角色快照元数据覆盖JSON对象；空对象表示无覆盖 |
| `resource_field_permissions` | ``resource_field_permissions JSON NOT NULL DEFAULT (JSON_OBJECT())`` | 固定资源字段权限快照，未声明字段不授予权限 |
| `version_name` | ``version_name VARCHAR(128) NULL`` | 版本显示名称；空值回退vN |
| `remark` | ``remark VARCHAR(512) NULL`` | 版本说明 |
| `metadata_version` | ``metadata_version BIGINT UNSIGNED NOT NULL DEFAULT 0`` | 独立元数据乐观锁 |
| `published_at` | ``published_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)`` | 权限快照发布时间点，UTC |

约束与索引：

```sql
PRIMARY KEY (id)
UNIQUE KEY uk_iam_revision_owner (id, role_id)
UNIQUE KEY uk_iam_revision_number (role_id, revision)
UNIQUE KEY uk_iam_revision_kind (id, kind)
CONSTRAINT fk_iam_revision_role FOREIGN KEY (role_id, kind) REFERENCES iam_role_definition (id, kind)
CONSTRAINT fk_iam_revision_base FOREIGN KEY (base_revision_id, base_kind) REFERENCES iam_role_revision (id, kind)
CONSTRAINT ck_iam_revision_number CHECK (revision > 0)
CONSTRAINT ck_iam_revision_base CHECK (base_revision_id IS NULL OR (kind = 'TENANT_CUSTOM' AND base_revision_id <> id))
CONSTRAINT ck_iam_revision_metadata CHECK (JSON_TYPE(metadata_overrides) = 'OBJECT')
CONSTRAINT ck_iam_revision_fields CHECK (JSON_TYPE(resource_field_permissions) = 'OBJECT')
```

## iam_role_parameter

角色快照范围参数定义

权威来源：[databases/iam/002_catalog_role.sql](../../../databases/iam/002_catalog_role.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `revision_id` | ``revision_id BIGINT UNSIGNED NOT NULL`` | 角色固定版本标识；引用归属由本表外键及业务事务共同校验 |
| `parameter_key` | ``parameter_key VARCHAR(128) COLLATE utf8mb4_bin NOT NULL`` | 快照内唯一的范围参数名称，用于分配参数绑定 |
| `resource_id` | ``resource_id BIGINT UNSIGNED NULL`` | 资源标识；引用归属由本表外键及业务事务共同校验 |
| `binding_kind` | ``binding_kind VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL`` | 范围参数绑定类别：DEPARTMENTS部门、OBJECTS业务对象；取值DEPARTMENTS,OBJECTS |

约束与索引：

```sql
PRIMARY KEY (revision_id, parameter_key)
CONSTRAINT fk_iam_parameter_revision FOREIGN KEY (revision_id) REFERENCES iam_role_revision (id)
CONSTRAINT fk_iam_parameter_resource FOREIGN KEY(resource_id) REFERENCES iam_resource(id)
CONSTRAINT ck_iam_parameter_kind CHECK (binding_kind IN ('DEPARTMENTS', 'OBJECTS'))
```

## iam_role_grant

角色快照操作授权

权威来源：[databases/iam/002_catalog_role.sql](../../../databases/iam/002_catalog_role.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `revision_id` | ``revision_id BIGINT UNSIGNED NOT NULL`` | 角色固定版本标识；引用归属由本表外键及业务事务共同校验 |
| `action_id` | ``action_id BIGINT UNSIGNED NOT NULL`` | 精确操作标识；引用归属由本表外键及业务事务共同校验 |
| `scopes` | ``scopes JSON NOT NULL`` | 授权或上限的范围表达式JSON数组；具体空数组语义由对应求值器判定 |

约束与索引：

```sql
PRIMARY KEY (revision_id, action_id)
CONSTRAINT fk_iam_grant_revision FOREIGN KEY (revision_id) REFERENCES iam_role_revision (id)
CONSTRAINT fk_iam_grant_action FOREIGN KEY (action_id) REFERENCES iam_action (id)
CONSTRAINT ck_iam_grant_scopes CHECK (JSON_TYPE(scopes) = 'ARRAY')
```

## iam_role_delta

历史租户角色定制差异

权威来源：[databases/iam/002_catalog_role.sql](../../../databases/iam/002_catalog_role.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `revision_id` | ``revision_id BIGINT UNSIGNED NOT NULL`` | 角色固定版本标识；引用归属由本表外键及业务事务共同校验 |
| `action_id` | ``action_id BIGINT UNSIGNED NOT NULL`` | 精确操作标识；引用归属由本表外键及业务事务共同校验 |
| `operation` | ``operation VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL`` | 历史定制差异操作：ADD增加、REMOVE移除、REPLACE_SCOPE替换范围 |
| `scopes` | ``scopes JSON NOT NULL`` | 授权或上限的范围表达式JSON数组；具体空数组语义由对应求值器判定 |

约束与索引：

```sql
PRIMARY KEY (revision_id, action_id)
CONSTRAINT fk_iam_delta_revision FOREIGN KEY (revision_id) REFERENCES iam_role_revision (id)
CONSTRAINT fk_iam_delta_action FOREIGN KEY (action_id) REFERENCES iam_action (id)
CONSTRAINT ck_iam_delta_operation CHECK (operation IN ('ADD', 'REMOVE', 'REPLACE_SCOPE'))
CONSTRAINT ck_iam_delta_scopes CHECK (JSON_TYPE(scopes) = 'ARRAY' AND (operation <> 'REMOVE' OR JSON_LENGTH(scopes) = 0))
```

## iam_delegation_grant

受限委派及期限上限

权威来源：[databases/iam/003_assignment_delegation.sql](../../../databases/iam/003_assignment_delegation.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `id` | ``id BIGINT UNSIGNED NOT NULL`` | 受限委派及期限上限记录标识 |
| `domain` | ``domain VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL`` | 授权管理域：PLATFORM平台、TENANT租户 |
| `tenant_id` | ``tenant_id BIGINT UNSIGNED NULL`` | 所属租户组织ID；NULL表示平台域或共享定义 |
| `tenant_key` | ``tenant_key BIGINT UNSIGNED GENERATED ALWAYS AS (COALESCE(tenant_id, 0)) STORED`` | 自动生成的归属唯一键：tenant_id为空取0，否则取租户ID；用于复合唯一索引及外键 |
| `platform_administrator_id` | ``platform_administrator_id BIGINT UNSIGNED NULL`` | 被委派的平台管理员成员ID，平台域使用 |
| `tenant_administrator_id` | ``tenant_administrator_id BIGINT UNSIGNED NULL`` | 被委派的租户管理员成员ID，租户域使用 |
| `valid_from` | ``valid_from DATETIME(6) NULL`` | 生效时间点，UTC；可空时表示不限制开始时间 |
| `valid_until` | ``valid_until DATETIME(6) NULL`` | 失效时间点，UTC；NULL表示不限制结束时间 |
| `assignment_duration_mode` | ``assignment_duration_mode VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'LIMITED'`` | 单次授权期限模式：LIMITED限制时长、UNLIMITED无上限；后者仅平台域可用 |
| `max_assignment_duration_seconds` | ``max_assignment_duration_seconds BIGINT UNSIGNED NULL`` | 单次授权最大时长的秒部分；UNLIMITED时为NULL |
| `max_assignment_duration_nanos` | ``max_assignment_duration_nanos INT UNSIGNED NOT NULL DEFAULT 0`` | 单次授权最大时长的纳秒部分，范围0至999999999 |
| `status` | ``status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ACTIVE'`` | 委派状态：ACTIVE有效、REVOKED已撤销；时效和派生授权仍按业务校验 |
| `version` | ``version BIGINT UNSIGNED NOT NULL DEFAULT 0`` | 记录乐观锁版本；变更时按所属业务事务递增 |
| `created_at` | ``created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)`` | 记录创建时间点，UTC |

约束与索引：

```sql
PRIMARY KEY (id)
UNIQUE KEY uk_iam_delegation_domain (domain, tenant_key, id)
UNIQUE KEY uk_iam_delegation_tenant (tenant_id, id)
KEY idx_iam_delegation_platform_admin (platform_administrator_id, status, valid_until)
KEY idx_iam_delegation_tenant_admin (tenant_id, tenant_administrator_id, status, valid_until)
CONSTRAINT fk_iam_delegation_platform_admin FOREIGN KEY (platform_administrator_id) REFERENCES iam_platform_member (id)
CONSTRAINT fk_iam_delegation_tenant_admin FOREIGN KEY (tenant_id, tenant_administrator_id) REFERENCES iam_tenant_member (tenant_id, id)
CONSTRAINT ck_iam_delegation_identity CHECK (
        (domain = 'PLATFORM' AND tenant_id IS NULL AND platform_administrator_id IS NOT NULL AND tenant_administrator_id IS NULL) OR
        (domain = 'TENANT' AND tenant_id IS NOT NULL AND tenant_administrator_id IS NOT NULL AND platform_administrator_id IS NULL))
CONSTRAINT ck_iam_delegation_status CHECK (status IN ('ACTIVE', 'REVOKED'))
CONSTRAINT ck_iam_delegation_interval CHECK (valid_from IS NULL OR valid_until IS NULL OR valid_from < valid_until)
CONSTRAINT ck_iam_delegation_duration CHECK (max_assignment_duration_nanos < 1000000000 AND
        ((assignment_duration_mode='LIMITED' AND max_assignment_duration_seconds IS NOT NULL
          AND (max_assignment_duration_seconds > 0 OR max_assignment_duration_nanos > 0))
         OR (assignment_duration_mode='UNLIMITED' AND domain='PLATFORM'
          AND max_assignment_duration_seconds IS NULL AND max_assignment_duration_nanos=0)))
```

## iam_delegation_role_revision

平台委派允许的固定角色版本

权威来源：[databases/iam/003_assignment_delegation.sql](../../../databases/iam/003_assignment_delegation.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `delegation_id` | ``delegation_id BIGINT UNSIGNED NOT NULL`` | 委派标识；引用归属由本表外键及业务事务共同校验 |
| `revision_id` | ``revision_id BIGINT UNSIGNED NOT NULL`` | 角色固定版本标识；引用归属由本表外键及业务事务共同校验 |
| `revision_kind` | ``revision_kind VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL`` | 被引用固定版本的角色种类，与版本ID共同验证引用合法性 |

约束与索引：

```sql
PRIMARY KEY (delegation_id, revision_id)
CONSTRAINT fk_iam_delegation_role_source FOREIGN KEY (delegation_id) REFERENCES iam_delegation_grant (id)
CONSTRAINT fk_iam_delegation_role_revision FOREIGN KEY (revision_id, revision_kind) REFERENCES iam_role_revision (id, kind)
```

## iam_delegation_recipient_member

委派允许的接收成员

权威来源：[databases/iam/003_assignment_delegation.sql](../../../databases/iam/003_assignment_delegation.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `delegation_id` | ``delegation_id BIGINT UNSIGNED NOT NULL`` | 委派标识；引用归属由本表外键及业务事务共同校验 |
| `domain` | ``domain VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL`` | 授权管理域：PLATFORM平台、TENANT租户 |
| `tenant_id` | ``tenant_id BIGINT UNSIGNED NULL`` | 所属租户组织ID；NULL表示平台域或共享定义 |
| `tenant_key` | ``tenant_key BIGINT UNSIGNED GENERATED ALWAYS AS (COALESCE(tenant_id, 0)) STORED`` | 自动生成的归属唯一键：tenant_id为空取0，否则取租户ID；用于复合唯一索引及外键 |
| `platform_member_id` | ``platform_member_id BIGINT UNSIGNED NULL`` | 平台域接收成员ID；非平台成员主体时为空 |
| `tenant_member_id` | ``tenant_member_id BIGINT UNSIGNED NULL`` | 租户域接收成员ID；非租户成员主体时为空 |
| `member_id` | ``member_id BIGINT UNSIGNED GENERATED ALWAYS AS (COALESCE(platform_member_id, tenant_member_id)) STORED`` | 自动生成的接收成员键：取platform_member_id或tenant_member_id；与delegation_id唯一，禁止重复接收者 |
| `id` | ``id BIGINT UNSIGNED NOT NULL`` | 委派接收成员关系记录标识；由IAM的Leaf发号器生成，不承载业务顺序 |

约束与索引：

```sql
PRIMARY KEY (id)
UNIQUE KEY uk_iam_delegation_recipient (delegation_id, member_id)
CONSTRAINT fk_iam_recipient_delegation FOREIGN KEY (domain, tenant_key, delegation_id) REFERENCES iam_delegation_grant (domain, tenant_key, id)
CONSTRAINT fk_iam_recipient_platform FOREIGN KEY (platform_member_id) REFERENCES iam_platform_member (id)
CONSTRAINT fk_iam_recipient_tenant FOREIGN KEY (tenant_id, tenant_member_id) REFERENCES iam_tenant_member (tenant_id, id)
CONSTRAINT ck_iam_recipient_identity CHECK (
        (domain = 'PLATFORM' AND tenant_id IS NULL AND platform_member_id IS NOT NULL AND tenant_member_id IS NULL) OR
        (domain = 'TENANT' AND tenant_id IS NOT NULL AND tenant_member_id IS NOT NULL AND platform_member_id IS NULL))
```

## iam_delegation_recipient_department

委派允许的接收部门

权威来源：[databases/iam/003_assignment_delegation.sql](../../../databases/iam/003_assignment_delegation.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `delegation_id` | ``delegation_id BIGINT UNSIGNED NOT NULL`` | 委派标识；引用归属由本表外键及业务事务共同校验 |
| `tenant_id` | ``tenant_id BIGINT UNSIGNED NOT NULL`` | 所属租户组织ID |
| `department_id` | ``department_id BIGINT UNSIGNED NOT NULL`` | 部门标识；引用归属由本表外键及业务事务共同校验 |
| `include_descendants` | ``include_descendants BOOLEAN NOT NULL DEFAULT FALSE`` | 选择该部门时是否包含下级部门成员：1包含、0仅直属 |

约束与索引：

```sql
PRIMARY KEY (delegation_id, department_id)
CONSTRAINT fk_iam_recipient_dept_delegation FOREIGN KEY (tenant_id, delegation_id) REFERENCES iam_delegation_grant (tenant_id, id)
CONSTRAINT fk_iam_recipient_dept_department FOREIGN KEY (tenant_id, department_id) REFERENCES iam_department (tenant_id, id)
CONSTRAINT ck_iam_recipient_descendants CHECK (include_descendants IN (0, 1))
```

## iam_delegation_action_ceiling

平台委派逐操作范围上限

权威来源：[databases/iam/003_assignment_delegation.sql](../../../databases/iam/003_assignment_delegation.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `delegation_id` | ``delegation_id BIGINT UNSIGNED NOT NULL`` | 委派标识；引用归属由本表外键及业务事务共同校验 |
| `action_id` | ``action_id BIGINT UNSIGNED NOT NULL`` | 精确操作标识；引用归属由本表外键及业务事务共同校验 |
| `scopes` | ``scopes JSON NOT NULL`` | 授权或上限的范围表达式JSON数组；具体空数组语义由对应求值器判定 |
| `scope_bindings` | ``scope_bindings JSON NOT NULL`` | 参数名到范围对象的绑定JSON对象；空对象表示无显式绑定 |

约束与索引：

```sql
PRIMARY KEY (delegation_id, action_id)
CONSTRAINT fk_iam_ceiling_delegation FOREIGN KEY (delegation_id) REFERENCES iam_delegation_grant (id)
CONSTRAINT fk_iam_ceiling_action FOREIGN KEY (action_id) REFERENCES iam_action (id)
CONSTRAINT ck_iam_ceiling_scopes CHECK (JSON_TYPE(scopes) = 'ARRAY')
CONSTRAINT ck_iam_ceiling_bindings CHECK (JSON_TYPE(scope_bindings) = 'OBJECT')
```

## iam_role_assignment

成员或用户组角色分配

权威来源：[databases/iam/003_assignment_delegation.sql](../../../databases/iam/003_assignment_delegation.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `id` | ``id BIGINT UNSIGNED NOT NULL`` | 成员或用户组角色分配记录标识 |
| `domain` | ``domain VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL`` | 授权管理域：PLATFORM平台、TENANT租户 |
| `tenant_id` | ``tenant_id BIGINT UNSIGNED NULL`` | 所属租户组织ID；NULL表示平台域或共享定义 |
| `tenant_key` | ``tenant_key BIGINT UNSIGNED GENERATED ALWAYS AS (COALESCE(tenant_id, 0)) STORED`` | 自动生成的归属唯一键：tenant_id为空取0，否则取租户ID；用于复合唯一索引及外键 |
| `subject_type` | ``subject_type VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL`` | 授权接收主体类别：MEMBER成员、GROUP用户组 |
| `platform_member_id` | ``platform_member_id BIGINT UNSIGNED NULL`` | 平台域接收成员ID；非平台成员主体时为空 |
| `platform_group_id` | ``platform_group_id BIGINT UNSIGNED NULL`` | 平台域接收用户组ID；非平台组主体时为空 |
| `tenant_member_id` | ``tenant_member_id BIGINT UNSIGNED NULL`` | 租户域接收成员ID；非租户成员主体时为空 |
| `tenant_group_id` | ``tenant_group_id BIGINT UNSIGNED NULL`` | 租户域接收用户组ID；非租户组主体时为空 |
| `role_id` | ``role_id BIGINT UNSIGNED NULL`` | 租户分配引用的角色身份ID；平台固定版本路径为空；撤销历史记录可保留旧版本引用 |
| `role_kind` | ``role_kind VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NULL`` | 被引用角色种类，与角色ID及归属共同验证合法性 |
| `role_tenant_key` | ``role_tenant_key BIGINT UNSIGNED NULL`` | 角色归属键：共享角色为0，租户私有角色为对应租户ID，配合复合外键校验 |
| `revision_id` | ``revision_id BIGINT UNSIGNED NULL`` | 平台分配引用的固定版本ID；租户有效分配按role_id求当前快照 |
| `revision_kind` | ``revision_kind VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NULL`` | 被引用固定版本的角色种类，与版本ID共同验证引用合法性 |
| `scope_bindings` | ``scope_bindings JSON NOT NULL`` | 参数名到范围对象的绑定JSON对象；空对象表示无显式绑定 |
| `delegation_grant_id` | ``delegation_grant_id BIGINT UNSIGNED NULL`` | 来源委派标识；引用归属由本表外键及业务事务共同校验 |
| `valid_from` | ``valid_from DATETIME(6) NOT NULL`` | 生效时间点，UTC；可空时表示不限制开始时间 |
| `valid_until` | ``valid_until DATETIME(6) NULL`` | 失效时间点，UTC；NULL表示不限制结束时间 |
| `status` | ``status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ACTIVE'`` | 分配状态：ACTIVE有效、REVOKED已撤销；有效期与主体状态还须独立校验 |
| `source` | ``source VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL`` | 记录来源，取值及对应来源标识由所属业务契约解释；取值MANUAL,INITIALIZATION,MIGRATION |
| `version` | ``version BIGINT UNSIGNED NOT NULL DEFAULT 0`` | 记录乐观锁版本；变更时按所属业务事务递增 |
| `created_at` | ``created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)`` | 记录创建时间点，UTC |

约束与索引：

```sql
PRIMARY KEY (id)
KEY ix_iam_assignment_revision_status (revision_id,status,id)
KEY idx_iam_assignment_platform_member (platform_member_id, status, valid_until)
KEY idx_iam_assignment_platform_group (platform_group_id, status, valid_until)
KEY idx_iam_assignment_tenant_member (tenant_id, tenant_member_id, status, valid_until)
KEY idx_iam_assignment_tenant_group (tenant_id, tenant_group_id, status, valid_until)
KEY idx_iam_assignment_delegation (delegation_grant_id, status)
CONSTRAINT fk_iam_assignment_platform_member FOREIGN KEY (platform_member_id) REFERENCES iam_platform_member (id)
CONSTRAINT fk_iam_assignment_platform_group FOREIGN KEY (platform_group_id) REFERENCES iam_platform_group (id)
CONSTRAINT fk_iam_assignment_tenant_member FOREIGN KEY (tenant_id, tenant_member_id) REFERENCES iam_tenant_member (tenant_id, id)
CONSTRAINT fk_iam_assignment_tenant_group FOREIGN KEY (tenant_id, tenant_group_id) REFERENCES iam_tenant_group (tenant_id, id)
CONSTRAINT fk_iam_assignment_revision FOREIGN KEY (revision_id, revision_kind) REFERENCES iam_role_revision (id, kind)
CONSTRAINT fk_iam_assignment_delegation FOREIGN KEY (domain, tenant_key, delegation_grant_id) REFERENCES iam_delegation_grant (domain, tenant_key, id)
KEY idx_iam_assignment_current_role (role_id, status)
CONSTRAINT fk_iam_assignment_current_role FOREIGN KEY (domain, role_tenant_key, role_id, role_kind)
        REFERENCES iam_role_definition (domain, tenant_key, id, kind)
CONSTRAINT ck_iam_assignment_current_role CHECK (
        (domain='PLATFORM' AND role_id IS NULL AND revision_id IS NOT NULL AND revision_kind IS NOT NULL) OR
        (domain='TENANT' AND role_id IS NOT NULL AND role_kind IS NOT NULL AND role_tenant_key IS NOT NULL AND
          ((role_kind='SHARED' AND role_tenant_key=0) OR
           (role_kind='TENANT_CUSTOM' AND role_tenant_key=tenant_id))) OR
        (domain='TENANT' AND status='REVOKED' AND role_id IS NULL AND revision_id IS NOT NULL))
CONSTRAINT ck_iam_assignment_subject CHECK (
        (domain = 'PLATFORM' AND tenant_id IS NULL AND tenant_member_id IS NULL AND tenant_group_id IS NULL AND
            ((subject_type = 'MEMBER' AND platform_member_id IS NOT NULL AND platform_group_id IS NULL) OR
             (subject_type = 'GROUP' AND platform_group_id IS NOT NULL AND platform_member_id IS NULL))) OR
        (domain = 'TENANT' AND tenant_id IS NOT NULL AND platform_member_id IS NULL AND platform_group_id IS NULL AND
            ((subject_type = 'MEMBER' AND tenant_member_id IS NOT NULL AND tenant_group_id IS NULL) OR
             (subject_type = 'GROUP' AND tenant_group_id IS NOT NULL AND tenant_member_id IS NULL))))
CONSTRAINT ck_iam_assignment_status CHECK (status IN ('ACTIVE', 'REVOKED'))
CONSTRAINT ck_iam_assignment_source CHECK (source IN ('MANUAL', 'INITIALIZATION', 'MIGRATION'))
CONSTRAINT ck_iam_assignment_interval CHECK (valid_until IS NULL OR valid_from < valid_until)
CONSTRAINT ck_iam_assignment_bindings CHECK (JSON_TYPE(scope_bindings) = 'OBJECT')
```

## iam_delegation_role

租户委派允许的角色身份

权威来源：[databases/iam/003_assignment_delegation.sql](../../../databases/iam/003_assignment_delegation.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `delegation_id` | ``delegation_id BIGINT UNSIGNED NOT NULL`` | 委派标识；引用归属由本表外键及业务事务共同校验 |
| `tenant_id` | ``tenant_id BIGINT UNSIGNED NOT NULL`` | 所属租户组织ID |
| `domain` | ``domain VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'TENANT'`` | 授权管理域：PLATFORM平台、TENANT租户 |
| `role_id` | ``role_id BIGINT UNSIGNED NOT NULL`` | 角色标识；引用归属由本表外键及业务事务共同校验 |
| `role_kind` | ``role_kind VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL`` | 被引用角色种类，与角色ID及归属共同验证合法性 |
| `role_tenant_key` | ``role_tenant_key BIGINT UNSIGNED NOT NULL`` | 角色归属键：共享角色为0，租户私有角色为对应租户ID，配合复合外键校验 |

约束与索引：

```sql
PRIMARY KEY (delegation_id, role_id)
CONSTRAINT fk_iam_live_delegation FOREIGN KEY (tenant_id, delegation_id) REFERENCES iam_delegation_grant (tenant_id, id)
CONSTRAINT fk_iam_live_delegation_role FOREIGN KEY (domain, role_tenant_key, role_id, role_kind)
        REFERENCES iam_role_definition (domain, tenant_key, id, kind)
CONSTRAINT ck_iam_live_delegation_role CHECK (domain='TENANT' AND
        ((role_kind='SHARED' AND role_tenant_key=0) OR (role_kind='TENANT_CUSTOM' AND role_tenant_key=tenant_id)))
```

## iam_delegation_resource_ceiling

租户委派逐资源范围上限

权威来源：[databases/iam/003_assignment_delegation.sql](../../../databases/iam/003_assignment_delegation.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `delegation_id` | ``delegation_id BIGINT UNSIGNED NOT NULL`` | 委派标识；引用归属由本表外键及业务事务共同校验 |
| `resource_id` | ``resource_id BIGINT UNSIGNED NOT NULL`` | 资源标识；引用归属由本表外键及业务事务共同校验 |
| `scopes` | ``scopes JSON NOT NULL`` | 授权或上限的范围表达式JSON数组；具体空数组语义由对应求值器判定 |
| `scope_bindings` | ``scope_bindings JSON NOT NULL`` | 参数名到范围对象的绑定JSON对象；空对象表示无显式绑定 |

约束与索引：

```sql
PRIMARY KEY (delegation_id, resource_id)
CONSTRAINT fk_iam_live_resource_delegation FOREIGN KEY (delegation_id) REFERENCES iam_delegation_grant (id)
CONSTRAINT fk_iam_live_resource FOREIGN KEY (resource_id) REFERENCES iam_resource (id)
CONSTRAINT ck_iam_live_resource_scopes CHECK (JSON_TYPE(scopes)='ARRAY')
CONSTRAINT ck_iam_live_resource_bindings CHECK (JSON_TYPE(scope_bindings)='OBJECT')
```

## iam_default_policy_revision

默认通讯录或字段策略固定版本

权威来源：[databases/iam/004_policy_audit_migration.sql](../../../databases/iam/004_policy_audit_migration.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `id` | ``id BIGINT UNSIGNED NOT NULL`` | 默认通讯录或字段策略固定版本记录标识 |
| `kind` | ``kind VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL`` | 默认策略类别：DIRECTORY通讯录、FIELD字段 |
| `revision` | ``revision BIGINT UNSIGNED NOT NULL`` | 不可变快照的发布序号，从1开始；与记录乐观锁版本不同 |
| `definition` | ``definition JSON NOT NULL`` | 默认策略不可变定义JSON对象，由对应通讯录或字段求值器解释 |

约束与索引：

```sql
PRIMARY KEY (id)
UNIQUE KEY uk_iam_default_policy_kind (id, kind)
UNIQUE KEY uk_iam_default_policy_revision (kind, revision)
CONSTRAINT ck_iam_default_policy_kind CHECK (kind IN ('DIRECTORY', 'FIELD'))
CONSTRAINT ck_iam_default_policy_definition CHECK (JSON_TYPE(definition) = 'OBJECT')
CONSTRAINT ck_iam_default_policy_revision CHECK (revision > 0)
```

## iam_policy_selector

租户策略人员选择器

权威来源：[databases/iam/004_policy_audit_migration.sql](../../../databases/iam/004_policy_audit_migration.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `id` | ``id BIGINT UNSIGNED NOT NULL`` | 租户策略人员选择器记录标识 |
| `tenant_id` | ``tenant_id BIGINT UNSIGNED NOT NULL`` | 所属租户组织ID |

约束与索引：

```sql
PRIMARY KEY (id)
UNIQUE KEY uk_iam_selector_tenant (tenant_id, id)
CONSTRAINT fk_iam_selector_tenant FOREIGN KEY (tenant_id) REFERENCES iam_tenant (id)
```

## iam_policy_selector_member

策略选择器显式成员

权威来源：[databases/iam/004_policy_audit_migration.sql](../../../databases/iam/004_policy_audit_migration.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `tenant_id` | ``tenant_id BIGINT UNSIGNED NOT NULL`` | 所属租户组织ID |
| `selector_id` | ``selector_id BIGINT UNSIGNED NOT NULL`` | 人员选择器标识；引用归属由本表外键及业务事务共同校验 |
| `member_id` | ``member_id BIGINT UNSIGNED NOT NULL`` | 成员标识；引用归属由本表外键及业务事务共同校验 |

约束与索引：

```sql
PRIMARY KEY (tenant_id, selector_id, member_id)
CONSTRAINT fk_iam_selector_member_selector FOREIGN KEY (tenant_id, selector_id) REFERENCES iam_policy_selector (tenant_id, id)
CONSTRAINT fk_iam_selector_member_member FOREIGN KEY (tenant_id, member_id) REFERENCES iam_tenant_member (tenant_id, id)
```

## iam_policy_selector_department

策略选择器部门来源

权威来源：[databases/iam/004_policy_audit_migration.sql](../../../databases/iam/004_policy_audit_migration.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `tenant_id` | ``tenant_id BIGINT UNSIGNED NOT NULL`` | 所属租户组织ID |
| `selector_id` | ``selector_id BIGINT UNSIGNED NOT NULL`` | 人员选择器标识；引用归属由本表外键及业务事务共同校验 |
| `department_id` | ``department_id BIGINT UNSIGNED NOT NULL`` | 部门标识；引用归属由本表外键及业务事务共同校验 |
| `include_descendants` | ``include_descendants BOOLEAN NOT NULL DEFAULT FALSE`` | 选择该部门时是否包含下级部门成员：1包含、0仅直属 |

约束与索引：

```sql
PRIMARY KEY (tenant_id, selector_id, department_id)
CONSTRAINT fk_iam_selector_dept_selector FOREIGN KEY (tenant_id, selector_id) REFERENCES iam_policy_selector (tenant_id, id)
CONSTRAINT fk_iam_selector_dept_dept FOREIGN KEY (tenant_id, department_id) REFERENCES iam_department (tenant_id, id)
CONSTRAINT ck_iam_selector_descendants CHECK (include_descendants IN (0, 1))
```

## iam_directory_policy

租户通讯录策略及默认版本引用

权威来源：[databases/iam/004_policy_audit_migration.sql](../../../databases/iam/004_policy_audit_migration.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `tenant_id` | ``tenant_id BIGINT UNSIGNED NOT NULL`` | 所属租户组织ID |
| `default_revision_id` | ``default_revision_id BIGINT UNSIGNED NOT NULL`` | 绑定的默认策略固定版本ID，与default_kind共同校验引用 |
| `default_kind` | ``default_kind VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'DIRECTORY'`` | 默认策略种类，固定为当前策略表对应的DIRECTORY或FIELD |
| `default_scope` | ``default_scope VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NULL`` | 空表示继承固定默认版本 |
| `default_selector_id` | ``default_selector_id BIGINT UNSIGNED NULL`` | 默认范围为SELECTED时的人员选择器ID，其他情况为空 |
| `version` | ``version BIGINT UNSIGNED NOT NULL DEFAULT 0`` | 记录乐观锁版本；变更时按所属业务事务递增 |

约束与索引：

```sql
PRIMARY KEY (tenant_id)
CONSTRAINT fk_iam_directory_policy_tenant FOREIGN KEY (tenant_id) REFERENCES iam_tenant (id)
CONSTRAINT fk_iam_directory_default FOREIGN KEY (default_revision_id, default_kind) REFERENCES iam_default_policy_revision (id, kind)
CONSTRAINT fk_iam_directory_default_selector FOREIGN KEY (tenant_id, default_selector_id) REFERENCES iam_policy_selector (tenant_id, id)
CONSTRAINT ck_iam_directory_default_kind CHECK (default_kind = 'DIRECTORY')
CONSTRAINT ck_iam_directory_default_scope CHECK (
        (default_scope IS NULL AND default_selector_id IS NULL) OR
        (default_scope IS NOT NULL AND ((default_scope IN ('ALL', 'SELF') AND default_selector_id IS NULL) OR
        (default_scope = 'SELECTED' AND default_selector_id IS NOT NULL))))
```

## iam_directory_rule

租户通讯录允许或拒绝规则

权威来源：[databases/iam/004_policy_audit_migration.sql](../../../databases/iam/004_policy_audit_migration.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `id` | ``id BIGINT UNSIGNED NOT NULL`` | 租户通讯录允许或拒绝规则记录标识 |
| `tenant_id` | ``tenant_id BIGINT UNSIGNED NOT NULL`` | 所属租户组织ID |
| `effect` | ``effect VARCHAR(8) CHARACTER SET ascii COLLATE ascii_bin NOT NULL`` | 通讯录规则效果：ALLOW允许、DENY拒绝；取值ALLOW,DENY |
| `viewer_selector_id` | ``viewer_selector_id BIGINT UNSIGNED NOT NULL`` | 规则适用的查看者选择器ID，须属于本租户 |
| `target_selector_id` | ``target_selector_id BIGINT UNSIGNED NOT NULL`` | 通讯录可见目标的选择器ID，须属于本租户 |

约束与索引：

```sql
PRIMARY KEY (id)
KEY idx_iam_directory_rule (tenant_id, effect, id)
CONSTRAINT fk_iam_directory_rule_policy FOREIGN KEY (tenant_id) REFERENCES iam_directory_policy (tenant_id)
CONSTRAINT fk_iam_directory_rule_viewer FOREIGN KEY (tenant_id, viewer_selector_id) REFERENCES iam_policy_selector (tenant_id, id)
CONSTRAINT fk_iam_directory_rule_target FOREIGN KEY (tenant_id, target_selector_id) REFERENCES iam_policy_selector (tenant_id, id)
CONSTRAINT ck_iam_directory_effect CHECK (effect IN ('ALLOW', 'DENY'))
```

## iam_field_policy

租户字段策略及操作规则

权威来源：[databases/iam/004_policy_audit_migration.sql](../../../databases/iam/004_policy_audit_migration.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `tenant_id` | ``tenant_id BIGINT UNSIGNED NOT NULL`` | 所属租户组织ID |
| `default_revision_id` | ``default_revision_id BIGINT UNSIGNED NOT NULL`` | 绑定的默认策略固定版本ID，与default_kind共同校验引用 |
| `default_kind` | ``default_kind VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'FIELD'`` | 默认策略种类，固定为当前策略表对应的DIRECTORY或FIELD |
| `operation_rules` | ``operation_rules JSON NOT NULL DEFAULT (JSON_ARRAY())`` | 租户字段操作规则JSON数组，限定已接入的编辑及筛选等能力 |
| `version` | ``version BIGINT UNSIGNED NOT NULL DEFAULT 0`` | 记录乐观锁版本；变更时按所属业务事务递增 |

约束与索引：

```sql
PRIMARY KEY (tenant_id)
CONSTRAINT fk_iam_field_policy_tenant FOREIGN KEY (tenant_id) REFERENCES iam_tenant (id)
CONSTRAINT fk_iam_field_default FOREIGN KEY (default_revision_id, default_kind) REFERENCES iam_default_policy_revision (id, kind)
CONSTRAINT ck_iam_field_default_kind CHECK (default_kind = 'FIELD')
CONSTRAINT ck_iam_field_operations CHECK (JSON_TYPE(operation_rules) = 'ARRAY')
```

## iam_field_rule

租户场景字段可见性规则

权威来源：[databases/iam/004_policy_audit_migration.sql](../../../databases/iam/004_policy_audit_migration.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `id` | ``id BIGINT UNSIGNED NOT NULL`` | 租户场景字段可见性规则记录标识 |
| `tenant_id` | ``tenant_id BIGINT UNSIGNED NOT NULL`` | 所属租户组织ID |
| `scenario` | ``scenario VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL`` | 字段策略使用场景：MANAGEMENT管理、DIRECTORY通讯录；取值MANAGEMENT,DIRECTORY |
| `field_key` | ``field_key VARCHAR(128) COLLATE utf8mb4_bin NOT NULL`` | 资源字段稳定标识，用于匹配目录能力及字段规则 |
| `viewer_selector_id` | ``viewer_selector_id BIGINT UNSIGNED NOT NULL`` | 规则适用的查看者选择器ID，须属于本租户 |
| `target_scope` | ``target_scope JSON NOT NULL`` | 字段规则目标范围表达式JSON数组 |
| `scope_bindings` | ``scope_bindings JSON NOT NULL`` | 参数名到范围对象的绑定JSON对象；空对象表示无显式绑定 |
| `visibility` | ``visibility VARCHAR(8) CHARACTER SET ascii COLLATE ascii_bin NOT NULL`` | 字段可见性：HIDDEN隐藏、MASKED脱敏、FULL完整；取值HIDDEN,MASKED,FULL |

约束与索引：

```sql
PRIMARY KEY (id)
KEY idx_iam_field_rule_match (tenant_id, scenario, field_key, id)
CONSTRAINT fk_iam_field_rule_policy FOREIGN KEY (tenant_id) REFERENCES iam_field_policy (tenant_id)
CONSTRAINT fk_iam_field_rule_viewer FOREIGN KEY (tenant_id, viewer_selector_id) REFERENCES iam_policy_selector (tenant_id, id)
CONSTRAINT ck_iam_field_rule_scenario CHECK (scenario IN ('MANAGEMENT', 'DIRECTORY'))
CONSTRAINT ck_iam_field_rule_visibility CHECK (visibility IN ('HIDDEN', 'MASKED', 'FULL'))
CONSTRAINT ck_iam_field_rule_scope CHECK (JSON_TYPE(target_scope) = 'ARRAY')
CONSTRAINT ck_iam_field_rule_bindings CHECK (JSON_TYPE(scope_bindings) = 'OBJECT')
```

## iam_member_export

租户成员导出任务与成员快照

权威来源：[databases/iam/004_policy_audit_migration.sql](../../../databases/iam/004_policy_audit_migration.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `id` | ``id BIGINT UNSIGNED NOT NULL`` | 租户成员导出任务与成员快照记录标识 |
| `tenant_id` | ``tenant_id BIGINT UNSIGNED NOT NULL`` | 所属租户组织ID |
| `actor_member_id` | ``actor_member_id BIGINT UNSIGNED NOT NULL`` | 发起操作的当前域成员ID |
| `tenant_version` | ``tenant_version VARCHAR(32) COLLATE utf8mb4_bin NOT NULL`` | 登记导出任务时的组织版本，取iam_tenant.version |
| `status` | ``status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL`` | 当前业务记录状态，合法值由本表CHECK及对应业务枚举限定；取值PENDING,RUNNING,SUCCEEDED,FAILED,EXPIRED |
| `member_ids` | ``member_ids JSON NULL`` | 导出成员ID的JSON快照数组；任务尚未生成快照时可空 |
| `failure_reason` | ``failure_reason VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL`` | 任务失败或阻塞的安全原因，不包含凭证及敏感原值 |
| `created_at` | ``created_at DATETIME(6) NOT NULL`` | 记录创建时间点，UTC |
| `completed_at` | ``completed_at DATETIME(6) NULL`` | 任务完成时间点，UTC；未完成时为空 |
| `expires_at` | ``expires_at DATETIME(6) NOT NULL`` | 到期时间点，UTC |

约束与索引：

```sql
PRIMARY KEY (id)
KEY idx_iam_member_export_tenant_status (tenant_id, status, expires_at)
CONSTRAINT ck_iam_member_export_status CHECK (status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'EXPIRED'))
CONSTRAINT ck_iam_member_export_ids CHECK (member_ids IS NULL OR JSON_TYPE(member_ids) = 'ARRAY')
```

## iam_member_workbook

租户成员表格导入导出任务

权威来源：[databases/iam/004_policy_audit_migration.sql](../../../databases/iam/004_policy_audit_migration.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `id` | ``id BIGINT UNSIGNED NOT NULL`` | 租户成员表格导入或导出任务标识 |
| `tenant_id` | ``tenant_id BIGINT UNSIGNED NOT NULL`` | 所属租户组织ID |
| `actor_member_id` | ``actor_member_id BIGINT UNSIGNED NOT NULL`` | 发起操作的当前域成员ID |
| `kind` | ``kind VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL`` | 任务种类；取值EXPORT,IMPORT |
| `mode` | ``mode VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL`` | 表格模式；取值CREATE,UPDATE |
| `status` | ``status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL`` | 当前业务记录状态，合法值由本表CHECK及对应业务枚举限定；取值PENDING,RUNNING,SUCCEEDED,FAILED,EXPIRED |
| `tenant_version` | ``tenant_version VARCHAR(32) COLLATE utf8mb4_bin NOT NULL`` | 登记任务时的组织版本，取iam_tenant.version |
| `member_ids` | ``member_ids JSON NULL`` | 导出范围成员ID的JSON数组；导入或尚未快照时可空 |
| `result` | ``result JSON NULL`` | 成功失败计数与逐行原因，不含初始密码明文 |
| `password_downloaded` | ``password_downloaded TINYINT(1) NOT NULL DEFAULT 0`` | 初始密码是否已一次性下载 |
| `failure_reason` | ``failure_reason VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL`` | 整份任务失败的安全原因，不包含凭证及敏感原值 |
| `created_at` | ``created_at DATETIME(6) NOT NULL`` | 记录创建时间点，UTC |
| `completed_at` | ``completed_at DATETIME(6) NULL`` | 任务完成时间点，UTC；未完成时为空 |
| `expires_at` | ``expires_at DATETIME(6) NOT NULL`` | 到期时间点，UTC |

约束与索引：

```sql
workbook MEDIUMBLOB NULL COMMENT '导出或上传校验后的xlsx工作簿'
error_report MEDIUMBLOB NULL COMMENT '逐行失败报告xlsx；无失败时为空'
password_file MEDIUMBLOB NULL COMMENT '新账号一次性初始密码xlsx；下载后清除'
PRIMARY KEY (id)
KEY idx_iam_member_workbook_actor (tenant_id, actor_member_id, created_at)
KEY idx_iam_member_workbook_status (tenant_id, status, expires_at)
CONSTRAINT ck_iam_member_workbook_kind CHECK (kind IN ('EXPORT', 'IMPORT'))
CONSTRAINT ck_iam_member_workbook_mode CHECK (mode IN ('CREATE', 'UPDATE'))
CONSTRAINT ck_iam_member_workbook_status CHECK (status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'EXPIRED'))
CONSTRAINT ck_iam_member_workbook_ids CHECK (member_ids IS NULL OR JSON_TYPE(member_ids) = 'ARRAY')
CONSTRAINT ck_iam_member_workbook_downloaded CHECK (password_downloaded IN (0, 1))
```

## iam_download_task

下载中心导出任务

权威来源：[databases/iam/004_policy_audit_migration.sql](../../../databases/iam/004_policy_audit_migration.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `id` | ``id BIGINT UNSIGNED NOT NULL`` | 下载任务标识 |
| `domain` | ``domain VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL`` | 发起人的认证域；取值PLATFORM,TENANT |
| `tenant_id` | ``tenant_id BIGINT UNSIGNED NULL`` | 所属租户组织ID；平台域为空 |
| `actor_member_id` | ``actor_member_id BIGINT UNSIGNED NOT NULL`` | 发起导出的当前域成员ID |
| `source` | ``source VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL`` | 导出来源编码，下载时据此复核权限 |
| `title` | ``title VARCHAR(128) NOT NULL`` | 列表展示名称 |
| `status` | ``status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL`` | 当前业务记录状态，合法值由本表CHECK及对应业务枚举限定；取值PENDING,RUNNING,SUCCEEDED,FAILED,EXPIRED |
| `filename` | ``filename VARCHAR(255) NULL`` | 下载文件名；成功后有值 |
| `content_type` | ``content_type VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NULL`` | 文件MIME类型 |
| `file_size` | ``file_size BIGINT UNSIGNED NULL`` | 文件字节数 |
| `object_path` | ``object_path VARCHAR(512) COLLATE utf8mb4_bin NULL`` | 对象存储路径bucket/objectName；过期删除后清空 |
| `failure_reason` | ``failure_reason VARCHAR(255) NULL`` | 可展示的失败原因，不包含异常堆栈及敏感原值 |
| `created_at` | ``created_at DATETIME(6) NOT NULL`` | 记录创建时间点，UTC |
| `started_at` | ``started_at DATETIME(6) NULL`` | 开始生成时间点，UTC |
| `completed_at` | ``completed_at DATETIME(6) NULL`` | 任务完成时间点，UTC；未完成时为空 |
| `expires_at` | ``expires_at DATETIME(6) NOT NULL`` | 文件到期时间点，UTC |

约束与索引：

```sql
PRIMARY KEY (id)
KEY idx_iam_download_task_owner (tenant_id, actor_member_id, created_at)
KEY idx_iam_download_task_status (status, expires_at)
CONSTRAINT ck_iam_download_task_domain CHECK (domain IN ('PLATFORM', 'TENANT'))
CONSTRAINT ck_iam_download_task_status CHECK (status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'EXPIRED'))
CONSTRAINT ck_iam_download_task_tenant CHECK ((domain = 'TENANT' AND tenant_id IS NOT NULL) OR (domain = 'PLATFORM' AND tenant_id IS NULL))
```

## iam_authorization_audit

授权与组织操作脱敏审计及投递状态

权威来源：[databases/iam/004_policy_audit_migration.sql](../../../databases/iam/004_policy_audit_migration.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `id` | ``id BIGINT UNSIGNED NOT NULL`` | 授权与组织操作脱敏审计及投递状态记录标识 |
| `event_id` | ``event_id VARCHAR(64) COLLATE utf8mb4_bin NOT NULL`` | 业务事件幂等标识，用于防止重复投递或重复保存 |
| `actor_account_id` | ``actor_account_id BIGINT UNSIGNED NOT NULL`` | 发起操作的全局账号ID |
| `actor_member_id` | ``actor_member_id BIGINT UNSIGNED NOT NULL`` | 发起操作的当前域成员ID |
| `domain` | ``domain VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL`` | 授权管理域：PLATFORM平台、TENANT租户 |
| `tenant_id` | ``tenant_id BIGINT UNSIGNED NULL`` | 审计所属租户ID；平台域为空，租户域必填 |
| `target_type` | ``target_type VARCHAR(64) COLLATE utf8mb4_bin NOT NULL`` | 审计或迁移目标的业务类型 |
| `target_id` | ``target_id VARCHAR(128) COLLATE utf8mb4_bin NOT NULL`` | 目标业务标识，具体类型由target_type或entity_type解释 |
| `change_type` | ``change_type VARCHAR(64) COLLATE utf8mb4_bin NOT NULL`` | 审计变更类型，对应AuditChangeType稳定字面量 |
| `safe_before` | ``safe_before JSON NOT NULL`` | 变更前脱敏摘要JSON对象，不保存凭证或敏感原值 |
| `safe_after` | ``safe_after JSON NOT NULL`` | 变更后脱敏摘要JSON对象，不保存凭证或敏感原值 |
| `revisions` | ``revisions JSON NOT NULL`` | 本次变更涉及的对象版本JSON对象 |
| `delegation_id` | ``delegation_id BIGINT UNSIGNED NULL`` | 委派标识；引用归属由本表外键及业务事务共同校验 |
| `assignment_id` | ``assignment_id BIGINT UNSIGNED NULL`` | 角色分配标识；引用归属由本表外键及业务事务共同校验 |
| `trace_id` | ``trace_id VARCHAR(128) NULL`` | 请求链路追踪标识，可空 |
| `occurred_at` | ``occurred_at DATETIME(6) NOT NULL`` | 业务发生时间点，UTC |
| `delivered_at` | ``delivered_at DATETIME(6) NULL`` | 审计投递完成时间点，UTC；NULL表示尚未确认投递 |

约束与索引：

```sql
PRIMARY KEY (id)
UNIQUE KEY uk_iam_audit_event (event_id)
KEY idx_iam_audit_tenant_time (tenant_id, occurred_at, id)
KEY idx_iam_audit_actor (actor_account_id, occurred_at, id)
KEY idx_iam_audit_assignment_create (assignment_id, change_type, occurred_at, id)
KEY idx_iam_audit_delivery (delivered_at, id)
CONSTRAINT ck_iam_audit_context CHECK ((domain = 'PLATFORM' AND tenant_id IS NULL) OR (domain = 'TENANT' AND tenant_id IS NOT NULL))
```

## iam_migration_batch

历史身份迁移批次及验收状态

权威来源：[databases/iam/004_policy_audit_migration.sql](../../../databases/iam/004_policy_audit_migration.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `id` | ``id VARCHAR(64) COLLATE utf8mb4_bin NOT NULL`` | 历史身份迁移批次及验收状态记录标识 |
| `source_fingerprint` | ``source_fingerprint CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL`` | 源数据结构或内容指纹，用于识别迁移输入 |
| `target_identifier` | ``target_identifier VARCHAR(128) COLLATE utf8mb4_bin NOT NULL`` | 明确选择的迁移目标标识，防止误用目标 |
| `rules_version` | ``rules_version VARCHAR(64) COLLATE utf8mb4_bin NOT NULL`` | 本批次采用的迁移规则版本 |
| `status` | ``status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'NEW'`` | 当前业务记录状态，合法值由本表CHECK及对应业务枚举限定；取值NEW,PREFLIGHTED,IMPORTED,VERIFIED,FAILED |
| `created_at` | ``created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)`` | 记录创建时间点，UTC |
| `verified_at` | ``verified_at DATETIME(6) NULL`` | 人工验收完成时间点，UTC；仅VERIFIED状态必填 |

约束与索引：

```sql
PRIMARY KEY (id)
CONSTRAINT ck_iam_batch_status CHECK (status IN ('NEW', 'PREFLIGHTED', 'IMPORTED', 'VERIFIED', 'FAILED'))
CONSTRAINT ck_iam_batch_verified CHECK ((status = 'VERIFIED' AND verified_at IS NOT NULL) OR (status <> 'VERIFIED' AND verified_at IS NULL))
```

## iam_migration_mapping

历史标识到目标实体的人工处置映射

权威来源：[databases/iam/004_policy_audit_migration.sql](../../../databases/iam/004_policy_audit_migration.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `batch_id` | ``batch_id VARCHAR(64) COLLATE utf8mb4_bin NOT NULL`` | 迁移批次标识；引用归属由本表外键及业务事务共同校验 |
| `source_table` | ``source_table VARCHAR(64) COLLATE utf8mb4_bin NOT NULL`` | 原始来源表名 |
| `source_id` | ``source_id VARCHAR(128) COLLATE utf8mb4_bin NOT NULL`` | 原始记录标识 |
| `target_type` | ``target_type VARCHAR(64) COLLATE utf8mb4_bin NOT NULL`` | 审计或迁移目标的业务类型 |
| `target_id` | ``target_id VARCHAR(128) COLLATE utf8mb4_bin NOT NULL`` | 目标业务标识，具体类型由target_type或entity_type解释 |
| `disposition` | ``disposition VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL`` | 人工迁移处置：KEEP_MAPPED/NARROW/RECONFIGURE/ARCHIVE/SKIP；取值KEEP_MAPPED,NARROW,RECONFIGURE,ARCHIVE,SKIP |
| `actor` | ``actor VARCHAR(128) NOT NULL`` | 作出处置的操作者标识 |
| `reason` | ``reason VARCHAR(1024) NOT NULL`` | 明确的人工处置依据 |

约束与索引：

```sql
PRIMARY KEY (batch_id, source_table, source_id, target_type, target_id)
CONSTRAINT fk_iam_mapping_batch FOREIGN KEY (batch_id) REFERENCES iam_migration_batch (id)
CONSTRAINT ck_iam_mapping_disposition CHECK (disposition IN ('KEEP_MAPPED', 'NARROW', 'RECONFIGURE', 'ARCHIVE', 'SKIP'))
```

## iam_migration_issue

历史迁移阻塞项及处置记录

权威来源：[databases/iam/004_policy_audit_migration.sql](../../../databases/iam/004_policy_audit_migration.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `id` | ``id BIGINT UNSIGNED NOT NULL`` | 历史迁移阻塞项及处置记录记录标识 |
| `batch_id` | ``batch_id VARCHAR(64) COLLATE utf8mb4_bin NOT NULL`` | 迁移批次标识；引用归属由本表外键及业务事务共同校验 |
| `issue_code` | ``issue_code VARCHAR(64) COLLATE utf8mb4_bin NOT NULL`` | 迁移问题稳定代码 |
| `source_table` | ``source_table VARCHAR(64) COLLATE utf8mb4_bin NULL`` | 原始来源表名 |
| `source_id` | ``source_id VARCHAR(128) COLLATE utf8mb4_bin NULL`` | 原始记录标识 |
| `safe_description` | ``safe_description VARCHAR(1024) NOT NULL`` | 迁移问题的脱敏说明 |
| `disposition` | ``disposition VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NULL`` | 人工迁移处置：KEEP_MAPPED/NARROW/RECONFIGURE/ARCHIVE/SKIP；取值KEEP_MAPPED,NARROW,RECONFIGURE,ARCHIVE,SKIP |
| `resolved_by` | ``resolved_by VARCHAR(128) NULL`` | 问题处置人；未处置时为空 |
| `resolution_reason` | ``resolution_reason VARCHAR(1024) NULL`` | 问题处置依据；未处置时为空 |

约束与索引：

```sql
PRIMARY KEY (id)
KEY idx_iam_issue_unresolved (batch_id, disposition, id)
CONSTRAINT fk_iam_issue_batch FOREIGN KEY (batch_id) REFERENCES iam_migration_batch (id)
CONSTRAINT ck_iam_issue_disposition CHECK (disposition IS NULL OR disposition IN ('KEEP_MAPPED', 'NARROW', 'RECONFIGURE', 'ARCHIVE', 'SKIP'))
CONSTRAINT ck_iam_issue_resolution CHECK (
        (disposition IS NULL AND resolved_by IS NULL AND resolution_reason IS NULL) OR
        (disposition IS NOT NULL AND resolved_by IS NOT NULL AND resolution_reason IS NOT NULL))
```

## iam_role_application_task

平台角色固定版本批量应用任务

权威来源：[databases/iam/004_policy_audit_migration.sql](../../../databases/iam/004_policy_audit_migration.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `id` | ``id BIGINT UNSIGNED NOT NULL PRIMARY KEY`` | 平台角色固定版本批量应用任务记录标识 |
| `role_id` | ``role_id BIGINT UNSIGNED NOT NULL`` | 角色标识；引用归属由本表外键及业务事务共同校验 |
| `target_revision_id` | ``target_revision_id BIGINT UNSIGNED NOT NULL`` | 目标角色固定版本标识；引用归属由本表外键及业务事务共同校验 |
| `actor_account_id` | ``actor_account_id BIGINT UNSIGNED NOT NULL`` | 发起操作的全局账号ID |
| `actor_member_id` | ``actor_member_id BIGINT UNSIGNED NOT NULL`` | 发起操作的当前域成员ID |
| `created_by` | ``created_by BIGINT UNSIGNED NOT NULL`` | 创建操作人标识 |
| `selection` | ``selection VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL`` | 版本应用选择方式：ALL/SOURCE_REVISIONS/ASSIGNMENTS；取值ALL,SOURCE_REVISIONS,ASSIGNMENTS |
| `selection_snapshot` | ``selection_snapshot JSON NOT NULL`` | 提交时冻结的分配选择条件JSON对象 |
| `status` | ``status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL`` | 当前业务记录状态，合法值由本表CHECK及对应业务枚举限定；取值PREPARING,READY,BLOCKED,RUNNING,PAUSED,SUCCEEDED |
| `version` | ``version BIGINT UNSIGNED NOT NULL DEFAULT 0`` | 记录乐观锁版本；变更时按所属业务事务递增 |
| `prepare_key` | ``prepare_key VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL`` | 同角色任务准备幂等键 |
| `confirm_key` | ``confirm_key VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NULL`` | 任务确认幂等键；未确认时为空 |
| `lease_owner` | ``lease_owner VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL`` | 领取任务的节点标识；无租约时为空 |
| `lease_until` | ``lease_until DATETIME(6) NULL`` | 任务租约失效时间点，UTC；无租约时为空 |
| `failure_reason` | ``failure_reason VARCHAR(512) NULL`` | 任务失败或阻塞的安全原因，不包含凭证及敏感原值 |
| `created_at` | ``created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)`` | 记录创建时间点，UTC |
| `updated_at` | ``updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)`` | 记录最后更新时间点，UTC |
| `running_role_id` | ``running_role_id BIGINT UNSIGNED GENERATED ALWAYS AS (CASE WHEN status='RUNNING' THEN role_id ELSE NULL END) STORED`` | 自动生成的运行角色键：RUNNING时取role_id，否则NULL；唯一索引限制同角色一个运行任务 |

约束与索引：

```sql
UNIQUE KEY uk_iam_role_application_prepare(role_id,prepare_key)
UNIQUE KEY uk_iam_role_application_running(running_role_id)
KEY ix_iam_role_application_role(role_id,id)
KEY ix_iam_role_application_claim(status,lease_until,id)
CONSTRAINT fk_iam_role_application_role FOREIGN KEY(role_id) REFERENCES iam_role_definition(id)
CONSTRAINT fk_iam_role_application_target FOREIGN KEY(target_revision_id) REFERENCES iam_role_revision(id)
CONSTRAINT ck_iam_role_application_status CHECK(status IN ('PREPARING','READY','BLOCKED','RUNNING','PAUSED','SUCCEEDED'))
CONSTRAINT ck_iam_role_application_selection CHECK(selection IN ('ALL','SOURCE_REVISIONS','ASSIGNMENTS'))
```

## iam_role_application_item

角色版本应用的分配明细

权威来源：[databases/iam/004_policy_audit_migration.sql](../../../databases/iam/004_policy_audit_migration.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `id` | ``id BIGINT UNSIGNED NOT NULL PRIMARY KEY`` | 角色版本应用的分配明细记录标识 |
| `task_id` | ``task_id BIGINT UNSIGNED NOT NULL`` | 版本应用任务标识；引用归属由本表外键及业务事务共同校验 |
| `assignment_id` | ``assignment_id BIGINT UNSIGNED NOT NULL`` | 角色分配标识；引用归属由本表外键及业务事务共同校验 |
| `original_snapshot` | ``original_snapshot JSON NOT NULL`` | 被选分配的原始快照JSON对象，用于预览与执行重验 |
| `expected_assignment_version` | ``expected_assignment_version BIGINT UNSIGNED NOT NULL`` | 执行前要求匹配的分配乐观锁版本 |
| `configured_bindings` | ``configured_bindings JSON NOT NULL`` | 任务内明确配置的范围参数JSON对象 |
| `preview_snapshot` | ``preview_snapshot JSON NULL`` | 最近一次预览结果JSON快照；未预览时为空 |
| `status` | ``status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL`` | 当前业务记录状态，合法值由本表CHECK及对应业务枚举限定；取值PENDING,BLOCKED,SUCCEEDED,EXCLUDED,REVOKED,EXPIRED,ALREADY_TARGET |
| `failure_reason` | ``failure_reason VARCHAR(512) NULL`` | 任务失败或阻塞的安全原因，不包含凭证及敏感原值 |

约束与索引：

```sql
UNIQUE KEY uk_iam_role_application_assignment(task_id,assignment_id)
KEY ix_iam_role_application_items(task_id,status,id)
CONSTRAINT fk_iam_role_application_item_task FOREIGN KEY(task_id) REFERENCES iam_role_application_task(id)
CONSTRAINT fk_iam_role_application_item_assignment FOREIGN KEY(assignment_id) REFERENCES iam_role_assignment(id)
CONSTRAINT ck_iam_role_application_item_status CHECK(status IN ('PENDING','BLOCKED','SUCCEEDED','EXCLUDED','REVOKED','EXPIRED','ALREADY_TARGET'))
```

## iam_live_authorization_batch

租户即时授权模型迁移批次

权威来源：[databases/iam/004_policy_audit_migration.sql](../../../databases/iam/004_policy_audit_migration.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `batch_id` | ``batch_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY`` | 迁移批次标识；引用归属由本表外键及业务事务共同校验 |
| `source_digest` | ``source_digest CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL`` | 即时授权迁移前的源内容摘要 |
| `reverse_digest` | ``reverse_digest CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL`` | 反向迁移核验摘要；尚未生成时为空 |
| `state` | ``state VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL`` | 迁移方向状态：FORWARD正向、REVERSE反向；取值FORWARD,REVERSE |
| `decisions` | ``decisions JSON NOT NULL DEFAULT (JSON_ARRAY())`` | 已确认迁移处置决策JSON数组 |
| `applied_at` | ``applied_at DATETIME(6) NULL`` | 迁移应用时间点，UTC；尚未应用时为空 |

约束与索引：

```sql
CONSTRAINT ck_iam_live_batch_state CHECK(state IN ('FORWARD','REVERSE'))
```

## iam_live_authorization_mapping

租户即时授权迁移原行快照与目标映射

权威来源：[databases/iam/004_policy_audit_migration.sql](../../../databases/iam/004_policy_audit_migration.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `batch_id` | ``batch_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL`` | 迁移批次标识；引用归属由本表外键及业务事务共同校验 |
| `entity_type` | ``entity_type VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL`` | 迁移记录对应的实体类型 |
| `entity_id` | ``entity_id BIGINT UNSIGNED NOT NULL`` | 迁移记录对应的实体标识 |
| `source_row` | ``source_row JSON NOT NULL`` | 迁移前原行JSON快照，仅供受控迁移与回退使用 |
| `target_id` | ``target_id BIGINT UNSIGNED NULL`` | 目标业务标识，具体类型由target_type或entity_type解释 |

约束与索引：

```sql
PRIMARY KEY(batch_id,entity_type,entity_id)
FOREIGN KEY(batch_id) REFERENCES iam_live_authorization_batch(batch_id)
```

## biz_leaf_alloc

Leaf业务号段分配高水位

权威来源：[databases/iam/005_auxiliary.sql](../../../databases/iam/005_auxiliary.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `biz_tag` | ```biz_tag` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT ''`` | Leaf业务号段名称，不同业务独立分配 |
| `max_id` | ```max_id` bigint NOT NULL DEFAULT '1'`` | 已分配号段的最大ID高水位，更新必须保持发号原子性 |
| `step` | ```step` int NOT NULL`` | 每次申请的号段长度 |
| `description` | ```description` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL`` | 业务说明，可空 |
| `update_time` | ```update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP`` | 号段高水位最近更新时间点，UTC |

约束与索引：

```sql
PRIMARY KEY (`biz_tag`)
```

## platform_dict

多作用域字典及字典项

权威来源：[databases/iam/005_auxiliary.sql](../../../databases/iam/005_auxiliary.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `id` | ```id` bigint NOT NULL`` | 多作用域字典及字典项记录标识 |
| `pid` | ```pid` bigint NOT NULL DEFAULT '0'`` | 父ID |
| `code` | ```code` varchar(64) NOT NULL`` | 编码 |
| `name` | ```name` varchar(128) NOT NULL`` | 名称 |
| `value` | ```value` varchar(128) DEFAULT NULL`` | 字典项值（仅字典项有效） |
| `label` | ```label` varchar(128) DEFAULT NULL`` | 字典项展示文本（仅字典项有效） |
| `type` | ```type` char(1) NOT NULL`` | 字典节点类别：0字典类型、1字典项 |
| `scope_type` | ```scope_type` char(1) NOT NULL DEFAULT '0'`` | 作用域, 0:平台,1:租户,2:应用 |
| `tenant_id` | ```tenant_id` bigint DEFAULT NULL`` | 租户ID（scope_type=1时必填） |
| `app_id` | ```app_id` bigint DEFAULT NULL`` | 作用域为应用时的IAM应用目录ID；其他作用域可空 |
| `org_type` | ```org_type` char(1) NOT NULL DEFAULT '0'`` | 组织类型：0平台、1组织 |
| `sort` | ```sort` int NOT NULL DEFAULT '0'`` | 排序权重 |
| `system_flag` | ```system_flag` bit(1) NOT NULL DEFAULT b'0'`` | 是否内置字典 |
| `status` | ```status` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '0'`` | 状态, 0:正常，9:禁用 |
| `remark` | ```remark` varchar(255) DEFAULT NULL`` | 备注 |
| `extra` | ```extra` json DEFAULT NULL`` | 扩展属性 |
| `created_by` | ```created_by` bigint DEFAULT NULL`` | 创建人 |
| `updated_by` | ```updated_by` bigint DEFAULT NULL`` | 更新人 |
| `created_at` | ```created_at` datetime DEFAULT NULL`` | 记录创建时间点，UTC |
| `updated_at` | ```updated_at` datetime DEFAULT NULL`` | 记录最后更新时间点，UTC |
| `deleted_at` | ```deleted_at` datetime DEFAULT NULL`` | 软删除时间点，UTC；NULL表示未删除 |

约束与索引：

```sql
PRIMARY KEY (`id`)
KEY `idx_dict_pid` (`pid`) USING BTREE
KEY `idx_dict_code` (`code`) USING BTREE
KEY `idx_dict_type_status` (`type`,`status`) USING BTREE
KEY `idx_dict_scope` (`scope_type`,`tenant_id`,`app_id`) USING BTREE
```

## security_event

统一安全事件（canonical）

权威来源：[databases/iam/005_auxiliary.sql](../../../databases/iam/005_auxiliary.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `id` | ```id` bigint NOT NULL AUTO_INCREMENT`` | 统一安全事件持久记录记录标识；数据库自增 |
| `event_id` | ```event_id` varchar(32) DEFAULT NULL`` | producer 幂等 ID |
| `event_type` | ```event_type` varchar(64) NOT NULL`` | 事件类型 |
| `event_category` | ```event_category` varchar(20) NOT NULL`` | AUTH/ACCOUNT/CREDENTIAL/ACCESS |
| `priority` | ```priority` varchar(16) NOT NULL DEFAULT 'BEST_EFFORT'`` | BEST_EFFORT/DURABLE |
| `occurred_at` | ```occurred_at` datetime DEFAULT NULL`` | 业务发生时间点，UTC |
| `received_at` | ```received_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP`` | 服务端接收安全事件的时间点，UTC |
| `tenant_id` | ```tenant_id` bigint DEFAULT NULL`` | 事件所属租户ID；NULL表示平台事件或未提供租户上下文 |
| `user_id` | ```user_id` bigint DEFAULT NULL`` | 安全事件关联的用户标识，结合user_type解释；可空 |
| `user_type` | ```user_type` varchar(20) DEFAULT NULL`` | 安全用户类型，对应UserTypeEnum：0系统账号、1会员账号 |
| `account` | ```account` varchar(128) DEFAULT NULL`` | 事件涉及的账号显示标识，可空 |
| `client_id` | ```client_id` varchar(64) DEFAULT NULL`` | 认证客户端标识，可空 |
| `app_id` | ```app_id` varchar(64) DEFAULT NULL`` | 上报方提供的应用标识，可空 |
| `session_id` | ```session_id` varchar(64) DEFAULT NULL`` | 认证会话标识，可空 |
| `device_id` | ```device_id` varchar(128) DEFAULT NULL`` | 设备标识，可空 |
| `client_ip` | ```client_ip` varchar(64) DEFAULT NULL`` | 客户端IP地址，可空 |
| `request_uri` | ```request_uri` varchar(512) DEFAULT NULL`` | 触发事件的请求路径，可空 |
| `user_agent` | ```user_agent` varchar(512) DEFAULT NULL`` | 客户端User-Agent，可空 |
| `result` | ```result` varchar(20) DEFAULT NULL`` | 安全操作结果稳定标识，可空 |
| `reason_code` | ```reason_code` varchar(50) DEFAULT NULL`` | 安全事件原因代码，可空 |
| `reason_detail` | ```reason_detail` varchar(500) DEFAULT NULL`` | 安全事件原因说明，不保存凭证原文 |
| `source_module` | ```source_module` varchar(64) NOT NULL`` | 上报模块 |
| `source` | ```source` varchar(50) DEFAULT NULL`` | 安全事件来源渠道或触发来源，可空 |
| `operator_id` | ```operator_id` bigint DEFAULT NULL`` | 操作人标识，可空 |
| `operator_name` | ```operator_name` varchar(64) DEFAULT NULL`` | 操作人姓名快照，可空 |
| `trace_id` | ```trace_id` varchar(64) DEFAULT NULL`` | 请求链路追踪标识，可空 |
| `extension` | ```extension` json DEFAULT NULL`` | 安全事件扩展JSON，须遵守脱敏白名单 |

约束与索引：

```sql
PRIMARY KEY (`id`)
UNIQUE KEY `uk_event_id` (`event_id`)
KEY `idx_received_id` (`received_at`,`id`)
KEY `idx_event_type` (`event_type`,`received_at`)
KEY `idx_tenant_user` (`tenant_id`,`user_id`)
KEY `idx_trace` (`trace_id`)
```

## sys_social_details

社交登录渠道配置

权威来源：[databases/iam/005_auxiliary.sql](../../../databases/iam/005_auxiliary.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `id` | ```id` bigint unsigned NOT NULL`` | 社交登录渠道配置记录标识 |
| `tenant_id` | ```tenant_id` bigint unsigned NOT NULL`` | 租户ID |
| `app_id` | ```app_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL`` | 第三方平台分配的客户端App ID，不是IAM应用目录数字ID |
| `app_secret` | ```app_secret` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL`` | 第三方应用密钥，可空；禁止在审计及字典示例中泄露 |
| `redirect_url` | ```redirect_url` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL`` | 重定向地址 |
| `name` | ```name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL`` | 社交名称 |
| `type` | ```type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL`` | 第三方登录渠道类型，对应社交登录渠道配置 |
| `status` | ```status` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0'`` | 状态, 0:正常，9:禁用 |
| `created_at` | ```created_at` datetime DEFAULT NULL`` | 记录创建时间点，UTC |
| `updated_at` | ```updated_at` datetime DEFAULT NULL`` | 记录最后更新时间点，UTC |
| `deleted_at` | ```deleted_at` datetime DEFAULT NULL`` | 软删除时间点，UTC；NULL表示未删除 |

约束与索引：

```sql
PRIMARY KEY (`id`) USING BTREE
```

## sys_user_social

全局账号社交身份绑定

权威来源：[databases/iam/005_auxiliary.sql](../../../databases/iam/005_auxiliary.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `id` | ```id` bigint NOT NULL`` | 全局账号社交身份绑定记录标识 |
| `tenant_id` | ```tenant_id` bigint NOT NULL`` | 组织ID |
| `user_id` | ```user_id` bigint NOT NULL`` | 被绑定的全局Account标识，旧列名保留；不表示租户成员ID |
| `type` | ```type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL`` | 渠道类型 |
| `unique_id` | ```unique_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL`` | 渠道唯一ID |
| `bind_at` | ```bind_at` datetime NOT NULL`` | 社交身份绑定时间点，UTC |

约束与索引：

```sql
PRIMARY KEY (`id`)
KEY `idx_unique_type_user` (`unique_id`,`type`,`user_id`) USING BTREE COMMENT '渠道用户索引'
```

## sys_tenant_plan_record

历史组织套餐购买记录

权威来源：[databases/iam/005_auxiliary.sql](../../../databases/iam/005_auxiliary.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `id` | ```id` bigint NOT NULL`` | 历史组织套餐购买记录记录标识 |
| `tenant_id` | ```tenant_id` bigint NOT NULL`` | 租户ID |
| `plan_id` | ```plan_id` int NOT NULL`` | 历史套餐标识，保留INT库存类型；不自动关联当前套餐目录 |
| `type` | ```type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL`` | 历史套餐类别：0长期、1短期 |
| `duration` | ```duration` int NOT NULL`` | 历史套餐购买时长数值，与unit共同表示有效时长 |
| `unit` | ```unit` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL`` | 历史套餐时长单位：1月、2年 |
| `created_at` | ```created_at` datetime NOT NULL`` | 记录创建时间点，UTC |

约束与索引：

```sql
PRIMARY KEY (`id`)
```

## account_lock_state

账号锁定状态表

权威来源：[ingot-framework/ingot-security/ingot-security-account/ingot-security-account-adapter/src/main/resources/sql/account_lock_state.sql](../../../ingot-framework/ingot-security/ingot-security-account/ingot-security-account-adapter/src/main/resources/sql/account_lock_state.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `id` | ``id              BIGINT       NOT NULL AUTO_INCREMENT`` | 安全框架账号锁定状态记录标识；数据库自增 |
| `user_id` | ``user_id         BIGINT       NOT NULL`` | 被锁定的安全用户ID，与user_type共同唯一标识用户 |
| `user_type` | ``user_type       VARCHAR(20)  NOT NULL DEFAULT '0'`` | 安全用户类型，对应UserTypeEnum：0系统账号、1会员账号 |
| `locked` | ``locked          TINYINT(1)   NOT NULL DEFAULT 0`` | 是否锁定（0-否 1-是） |
| `lock_type` | ``lock_type       VARCHAR(20)  DEFAULT NULL`` | 锁定类型（MANUAL-手动 AUTO-自动） |
| `lock_reason_code` | ``lock_reason_code   VARCHAR(50)  DEFAULT NULL`` | 锁定原因代码 |
| `lock_reason_detail` | ``lock_reason_detail VARCHAR(500) DEFAULT NULL`` | 锁定原因详情 |
| `locked_at` | ``locked_at       DATETIME     DEFAULT NULL`` | 锁定时间；时间点按UTC保存 |
| `locked_until` | ``locked_until    DATETIME     DEFAULT NULL`` | 锁定到期时间（NULL=永久锁定）；时间点按UTC保存 |
| `operator_id` | ``operator_id     BIGINT       DEFAULT NULL`` | 操作人ID |
| `operator_name` | ``operator_name   VARCHAR(64)  DEFAULT NULL`` | 操作人姓名 |
| `failed_login_count` | ``failed_login_count INT        NOT NULL DEFAULT 0`` | 连续登录失败次数 |
| `last_failed_at` | ``last_failed_at  DATETIME     DEFAULT NULL`` | 最后一次失败时间；时间点按UTC保存 |
| `created_at` | ``created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP`` | 记录创建时间点，UTC |
| `updated_at` | ``updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP`` | 记录最后更新时间点，UTC |

约束与索引：

```sql
PRIMARY KEY (id)
UNIQUE KEY uk_user_id_type (user_id, user_type)       COMMENT '用户ID + 用户类型联合唯一'
KEY idx_locked (locked, locked_until) USING BTREE     COMMENT '锁定状态 + 到期时间索引（自动解锁任务使用）'
```

## password_history

密码历史记录（环形缓冲）

权威来源：[ingot-framework/ingot-security/ingot-security-credential-data/src/main/resources/sql/add_password_history.sql](../../../ingot-framework/ingot-security/ingot-security-credential-data/src/main/resources/sql/add_password_history.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `id` | ```id` BIGINT NOT NULL AUTO_INCREMENT`` | 安全框架密码历史环形缓冲记录标识；数据库自增 |
| `user_id` | ```user_id` BIGINT NOT NULL`` | 安全框架全局用户ID；同用户按sequence_number维护环形历史 |
| `password_hash` | ```password_hash` VARCHAR(255) NOT NULL`` | 不可逆密码哈希，禁止作为响应明文返回 |
| `sequence_number` | ```sequence_number` INT NOT NULL`` | 序号（用于环形缓冲，从1开始） |
| `version` | ```version` bigint NOT NULL DEFAULT '1'`` | 记录乐观锁版本；变更时按所属业务事务递增 |
| `created_at` | ```created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP`` | 记录创建时间点，UTC |
| `updated_at` | ```updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP`` | 记录最后更新时间点，UTC |

约束与索引：

```sql
PRIMARY KEY (`id`)
UNIQUE KEY `uk_user_sequence` (`user_id`, `sequence_number`)
KEY `idx_created_at` (`created_at`)
```

## password_expiration

密码过期信息

权威来源：[ingot-framework/ingot-security/ingot-security-credential-data/src/main/resources/sql/add_password_history.sql](../../../ingot-framework/ingot-security/ingot-security-credential-data/src/main/resources/sql/add_password_history.sql)

| 字段 | 定义 | 说明 |
|---|---|---|
| `id` | ```id` BIGINT NOT NULL AUTO_INCREMENT`` | 安全框架密码过期状态记录标识；数据库自增 |
| `user_id` | ```user_id` BIGINT NOT NULL`` | 安全框架全局用户ID，唯一对应一份密码过期状态 |
| `last_changed_at` | ```last_changed_at` DATETIME NOT NULL`` | 最后修改密码时间；时间点按UTC保存 |
| `expires_at` | ```expires_at` DATETIME NOT NULL`` | 到期时间点，UTC |
| `force_change` | ```force_change` TINYINT(1) NOT NULL DEFAULT 0`` | 是否强制修改（0-否 1-是） |
| `grace_login_remaining` | ```grace_login_remaining` INT NOT NULL DEFAULT 0`` | 剩余宽限登录次数 |
| `next_warning_at` | ```next_warning_at` DATETIME NULL`` | 下次提醒时间；时间点按UTC保存 |
| `created_at` | ```created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP`` | 记录创建时间点，UTC |
| `updated_at` | ```updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP`` | 记录最后更新时间点，UTC |

约束与索引：

```sql
PRIMARY KEY (`id`)
UNIQUE KEY `uk_user_id` (`user_id`)
KEY `idx_expires_at` (`expires_at`)
KEY `idx_next_warning_at` (`next_warning_at`)
```
