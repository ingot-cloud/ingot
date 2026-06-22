# PMS 应用授权旧字段破坏性清理（发布 B）

> 状态：completed（DDL 已在目标库执行并验证，归档于 `archive/2026/`）

## 实施策略

承接已归档变更 `20260612-pms-application-authorization` 中拆出的**发布 B**：在应用中心化模型（发布 A）稳定运行一个完整发布周期后，执行不可逆的破坏性 DDL，删除旧字段、补齐非空约束与唯一/索引，使应用中心化模型成为唯一事实来源。本变更必须**独立发布**，不可与其他功能同批上线，回滚依赖数据库备份与反向迁移脚本。

## 元数据

| 项 | 值 |
|---|---|
| Change ID | `20260622-pms-authorization-ddl-cleanup` |
| 领域 | `pms` |
| 负责人 | TBD |
| 创建日期 | 2026-06-22 |
| 目标发布日期 | TBD（发布 A 稳定一个周期后） |
| 关联模块 | `ingot-pms`、`databases/migrations`、`databases/ingot_core.sql` |
| 前序变更 | `specs/changes/archive/2026/20260612-pms-application-authorization/` |
| 目标 Current | `specs/current/pms/application-authorization/`（验收后更新） |

## 目标

删除应用中心化改造保留的兼容旧字段与物理列，补齐非空与唯一/索引约束，移除最后的过渡逻辑，确保全新库可仅靠初始化脚本启动，旧版本可经迁移脚本升级、可经反向脚本回滚。

## 范围

### 包含

- 破坏性 DDL：DROP 旧列、SET NOT NULL、唯一/索引约束（见 [DESIGN](./DESIGN.md)）。
- 迁移脚本 `008_application_authorization_cleanup.sql` 与 `rollback_008.sql`。
- 初始化脚本 `databases/ingot_core.sql` 同步。
- 数据库初始化脚本、API、权限编码规范、应用接入/租户开通/审计/回滚等文档同步。

### 不包含

- 任何业务逻辑或接口行为变更（发布 A 已完成）。
- 删除 `platform_role` 布尔列（按 D2 保留为角色来源唯一表示）。
- `role_source` 相关清理（已在发布 A §0.4 整体删除，无遗留物理列）。

## 工件

| 文档 | 内容 |
|---|---|
| [需求](./REQUIREMENTS.md) | 前置条件、约束、非目标与验收标准 |
| [设计](./DESIGN.md) | DDL 清单、约束、迁移与回滚方案 |
| [任务](./TASKS.md) | 前置闸门与 DDL 任务、验收条件 |

## 完成记录

- 完成日期：2026-06-22
- 关联提交或 PR：见本次归档提交
- 更新的 current capability：`specs/current/pms/application-authorization/`（移除「保留物理列」过渡说明，标注旧列已删除）
- 与原设计的差异：
  - 破坏性 DDL 已在目标数据库执行并验证；迁移脚本（`008_application_authorization_cleanup.sql` / `rollback_008.sql`）与 `databases/ingot_core.sql` 初始化脚本的同步**另行处理**，未随本次归档提交。
- 取消原因：不适用（已完成）。

> 备注：归档时仓库 `databases/migrations/` 尚无 `008` 脚本、`databases/ingot_core.sql` 仍含旧列定义，按负责人确认（DDL 已在库中执行、脚本另行处理）归档为 completed。补齐仓库脚本属后续运维事项。
