# Design

## 方案摘要

通过一次独立、可回滚的破坏性 DDL 发布，删除应用中心化改造（发布 A）保留的兼容旧列，并补齐非空与唯一/索引约束。发布 A 已完成所有 Java 层引用移除（`enable_permission`、`org_type` 派生、`menu_id/permission_id` 读路径、`role_source`），本次仅处理物理层。

## 数据模型与接口

### DROP 列

```text
platform_app.menu_id
platform_app.permission_id
platform_menu.org_type
platform_menu.enable_permission
platform_permission.org_type
```

> 不删除 `platform_role` 布尔列（D2，角色来源唯一表示）。`role_source` 列已在发布 A 整体移除，本次无需处理。

### SET NOT NULL

```text
platform_menu.app_id
platform_menu.access_mode
platform_menu.permission_id
platform_permission.app_id
platform_permission.node_type
platform_permission.source_type
```

### 唯一 / 索引

```text
UNIQUE platform_app(code)
UNIQUE platform_permission(code)
UNIQUE tenant_app_config(tenant_id, app_id)
UNIQUE role permission relation
INDEX  platform_menu(app_id, pid, status, sort)
INDEX  platform_permission(app_id, pid, status)
```

如启用外键策略，可对应用、菜单、权限增加受控外键；否则由领域服务与审计任务保证完整性。

### 脚本与文档

- 新增 `databases/migrations/008_application_authorization_cleanup.sql`、`databases/migrations/rollback_008.sql`。
- 同步 `databases/ingot_core.sql` 初始化脚本与种子数据。
- 同步 API 文档、权限编码规范、应用接入/租户开通/审计/回滚手册。

## 数据流与失败处理

- 执行前先运行只读审计（`GET /v1/platform/dev/authorization/audit`），确认无 `app_id`/来源/节点类型为空的数据，否则先回填再执行 DDL。
- SET NOT NULL 前必须确认对应列零空值，避免 DDL 中途失败。
- 唯一约束前必须确认无重复 `code` / 重复关系，否则先清理重复数据。

## 迁移与回滚

| 步骤 | 行为 | 回滚方式 |
|---|---|---|
| 备份 | 全量数据库备份 + 回滚演练 | —— |
| 升级 | 执行 `008` | `rollback_008` + 备份恢复 |

本变更属破坏性清理，不能仅通过配置切回旧模型，必须依赖：数据库备份恢复、上一版本应用镜像、已验证的反向迁移脚本。因此必须独立发布。

## 测试策略

- 迁移测试：测试/预发布库执行 `008` 与 `rollback_008`，校验列删除、非空、唯一/索引生效。
- 安全测试：未授权应用权限不可提交、平台默认权限不可被租户删除、通配授权审计完整。
- 回归测试：平台/租户用户菜单、角色授权、应用启停全链路。
- 初始化测试：全新库仅靠 `ingot_core.sql` 启动并通过冒烟。
