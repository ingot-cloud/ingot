# 阶段 1：兼容性数据库扩展

> 状态：代码与迁移脚本已完成，待测试库执行与统一验收。

## 1. 目标

通过新增字段和索引建立应用中心化模型，完成数据回填，但继续由旧模型对外提供授权结果。

## 2. 数据库变更

### 2.1 `platform_app`

新增：

```text
code varchar(64)
app_type char(1)
sort int
```

迁移期间允许 `code`、`app_type` 为空；回填完成后改为非空，并增加：

```text
UNIQUE(code)
INDEX(app_type, status, sort)
```

### 2.2 `platform_menu`

新增：

```text
app_id bigint NULL
access_mode char(1) NULL
```

索引：

```text
INDEX(app_id, pid, status, sort)
```

本阶段保留 `org_type`、`enable_permission` 和原 `permission_id`。

### 2.3 `platform_permission`

新增：

```text
app_id bigint NULL
node_type char(1) NULL
source_type char(1) NULL
source_id bigint NULL
managed bit NOT NULL DEFAULT 0
```

索引：

```text
UNIQUE(code)
INDEX(app_id, pid, status)
INDEX(source_type, source_id)
```

### 2.4 `tenant_app_config`

新增：

```text
source varchar(32) NULL
valid_from datetime NULL
valid_until datetime NULL
```

增加唯一约束：

```text
UNIQUE(tenant_id, app_id)
```

### 2.5 角色关联

为后续替代布尔多态字段新增：

```text
role_source char(1) NULL
```

涉及：

- `tenant_role_user_private`
- `tenant_role_permission_private`

迁移期间双写 `platform_role` 和 `role_source`。

## 3. 数据回填

### 3.1 应用回填

- 为现有租户应用生成稳定 `code` 和 `app_type=TENANT`。
- 根据平台菜单一级模块创建 `PLATFORM` 应用。
- 平台应用编码使用稳定业务编码，不直接使用中文名称。

### 3.2 菜单归属

- 从现有应用根菜单递归回填租户菜单 `app_id`。
- 平台菜单按一级模块归入对应平台应用。
- 同一菜单树出现多个应用归属时停止迁移并输出异常。

### 3.3 权限归属

- 菜单权限继承菜单 `app_id`。
- 菜单权限标记为 `NAVIGATION + MENU + managed=true`。
- 现有手工权限根据父权限继承 `app_id`，标记为 `ACTION + MANUAL`。
- 每个应用创建唯一的 `app_code:*` 根权限。

### 3.4 访问模式

```text
enable_permission=true  → PERMISSION
enable_permission=false → OPEN
```

### 3.5 角色来源

```text
platform_role=true  → PLATFORM
platform_role=false → TENANT
```

## 4. 兼容策略

- 旧字段仍然是线上读路径的事实来源。
- 新增写操作同时写入新旧字段。
- 回填脚本必须可重复执行。
- 每次回填都输出处理数量、跳过数量和异常数量。
- 回填失败不能留下半棵菜单树已迁移的状态。

## 5. 验收标准

- 每个应用都有唯一非空 `code`、明确 `app_type` 和根权限。
- 每个有效菜单都有唯一 `app_id`。
- 每个有效权限都有唯一 `app_id`。
- 菜单与其托管权限的 `app_id` 一致。
- 子菜单与父菜单的 `app_id` 一致。
- 子权限与父权限的 `app_id` 一致。
- `access_mode` 与旧 `enable_permission` 完全对应。
- `role_source` 与旧布尔字段完全对应。
- 新增唯一索引前不存在重复数据。
- 旧模型的权限和菜单返回结果没有变化。

## 6. 退出条件

- 测试、预发布数据库回填成功。
- 数据审计无阻断级异常。
- 新字段已完成双写。

## 7. 回滚

- 线上仍读旧字段，因此可以回滚应用代码。
- 回滚时保留新增字段和回填数据，不执行破坏性 DDL。
- 新增唯一索引如影响旧写入，应先修复旧写入逻辑，不通过删除数据规避。
