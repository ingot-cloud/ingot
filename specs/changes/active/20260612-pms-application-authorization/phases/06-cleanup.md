# 阶段 6：旧模型清理

> 状态：待实施。

## 1. 目标

在新模型稳定运行后删除旧字段、旧接口和兼容分支，确保应用中心化模型成为唯一事实来源。

## 2. 前置条件

必须同时满足：

- 新模型稳定运行至少一个完整发布周期。
- 所有租户均已切换。
- 影子差异记录持续为零。
- 没有旧版本服务实例运行。
- 已完成数据库备份和回滚演练。
- 已确认没有外部系统读取待删除字段。

## 3. 清理内容

### 3.1 应用

删除：

```text
platform_app.menu_id
platform_app.permission_id
```

### 3.2 菜单

删除：

```text
platform_menu.org_type
platform_menu.enable_permission
```

将以下字段改为非空：

```text
platform_menu.app_id
platform_menu.access_mode
platform_menu.permission_id
```

### 3.3 权限

删除：

```text
platform_permission.org_type
```

将以下字段改为非空：

```text
platform_permission.app_id
platform_permission.node_type
platform_permission.source_type
```

### 3.4 角色关联

删除旧 `platform_role` 布尔字段，正式使用 `role_source`。

### 3.5 代码

删除：

- 旧应用根菜单和根权限推导逻辑。
- 普通父权限自动包含后代的逻辑。
- `org_type` 菜单和权限过滤。
- `enable_permission` 分支。
- 双写和双读兼容代码。
- 已废弃的管理接口。
- 旧模型功能开关。

## 4. 数据约束

最终增加或确认：

```text
UNIQUE platform_app(code)
UNIQUE platform_permission(code)
UNIQUE tenant_app_config(tenant_id, app_id)
UNIQUE role permission relation
INDEX platform_menu(app_id, pid, status, sort)
INDEX platform_permission(app_id, pid, status)
```

如数据库支持并启用外键策略，可对应用、菜单和权限增加受控外键；否则必须由领域服务和审计任务持续保证完整性。

## 5. 文档与运维

更新：

- 数据库初始化脚本。
- API 文档。
- 权限编码规范。
- 应用接入指南。
- 租户开通应用操作指南。
- 角色授权审计说明。
- 故障回滚手册。

## 6. 验收标准

- 代码中不再读取已删除字段。
- 数据库不存在 `app_id`、权限来源或角色来源为空的数据。
- 所有权限都能追溯到应用。
- 所有菜单权限都能追溯到菜单。
- 所有租户应用授权都有唯一关系。
- 全量测试、权限安全测试和迁移回归测试通过。
- 全新数据库可以仅通过最新初始化脚本正确启动。
- 从上一正式版本升级可以通过迁移脚本完成。

## 7. 回滚

本阶段属于破坏性清理，不能只通过配置切回旧模型。

回滚依赖：

- 数据库备份恢复。
- 上一版本应用镜像。
- 已验证的反向迁移脚本。

因此阶段 6 必须独立发布，不能与阶段 5 同批上线。
