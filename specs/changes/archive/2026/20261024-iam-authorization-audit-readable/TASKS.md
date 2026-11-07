# Tasks

## 实施任务

- [x] T1：公共模型
  - 依赖：无
  - 内容：在 `ingot-commons` 中新增 `AuditCursorPage`、`AuditReference`、`AuditReferenceState`（形态对齐 `CommonStatusEnum`）和 `AuditReferenceType` 常量，补全 Javadoc。
  - 验收：枚举序列化与 `getEnum` 的测试通过，OpenAPI 快照已更新。
- [x] T2：索引迁移
  - 依赖：无
  - 内容：新增 `032_authorization_audit_cursor_index.sql` 及其 rollback；同步 `ingot_iam.sql`、004、026 和迁移 README。
  - 验收：MySQL 8 上重复执行、回滚均通过，数据库来源回归通过。
- [x] T3：游标查询
  - 依赖：T1
  - 内容：`DiagnoseAuditService.listAudits` 改为键集查询并移除 COUNT；新增游标编解码；两个 API 改为 `limit`、`cursor` 参数。
  - 验收：H2 集成测试覆盖同时间戳跨页、筛选组合、租户隔离、非法游标 400，MVC 测试通过。
- [x] T4：名称解析
  - 依赖：T1、T3
  - 内容：实现 `AuditReferenceResolver`，以及 DESIGN 中各类型的 `AuditReferenceSource`；把现有 `actorNames` 迁入 member 来源；逐一核对各类型所用的读取 ACTION 并记录在此处。
  - 验收：三种状态、字段策略、数据范围、快照名称、租户账号映射的测试全部通过；每页 SQL 次数有固定上限。
- [x] T5：写入侧修正
  - 依赖：无
  - 内容：批量任务类审计不再写 NAME 编码；平台委派写入真实的接收对象。
  - 验收：对应服务的测试断言新的差异值；读取历史占位值不报错。
- [x] T6：契约同步
  - 依赖：T3、T4
  - 内容：更新接口快照、Python 契约，以及前端 change 的 `API.md`。
  - 验收：快照校验、契约回归通过。
  - 记录：重建 OpenAPI 时补导出了既有的 `MemberWorkbookFile`、`MemberWorkbookTask`、`MemberWorkbookHistoryItem`、`DepartmentWorkbookExportInput`，快照随之同步部门导入导出等已存在接口；`test_management_controllers_match_published_routes_excluding_oss_and_inner` 因成员导入、下载中心等 14 条既有路由未登记而失败，与本变更无关。

## 验证任务

- [x] V1：IAM provider 定向回归，包括审计、目录、角色、分配、组织和成员相关测试。
  - 记录：全量 provider 测试 276 个中 18 个失败，均为 `AuthorizationEvaluatorTest`、`RoleFieldPermissionServiceTest`，在未改动的 HEAD 上同样失败，与本变更无关。
- [x] V2：2026-11-07 在隔离 MySQL 8.4.13 导入 15 万条合成审计，其中主测试组织 10 万、另一组织 2.5 万、平台 2.5 万。对两个域分别执行三类查询的首屏和带游标 `EXPLAIN FORMAT=JSON`，共 12 个计划。时间、操作者及对象类型与 ID 分别命中 `idx_iam_audit_tenant_time`、`idx_iam_audit_tenant_actor_time`、`idx_iam_audit_tenant_target_time`。首屏为 `ref`，游标页为 `range`，均无 filesort 或全表扫描。032 重复执行和回滚通过，审计行数保持 15 万。
- [x] V3：2026-11-07 用户确认两站前端联调验收通过，覆盖翻页、筛选、名称展示，以及无权限和已删除对象的展示。

## 完成检查

- [x] 实现与 DESIGN 一致，实施时补充的决策已记录。
- [x] 审计功能验收标准满足。
- [x] 更新平台能力与数据库持久化的 current 基线。
- [x] 主 IAM change 的 AUTHORIZATION-AUDIT-REFINEMENT 已注明被本变更部分替代。
- [x] Change 已记录完成信息并归档。
