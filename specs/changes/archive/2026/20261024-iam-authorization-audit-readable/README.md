# 授权审计游标分页与可读展示

> 状态：completed
> 批准记录：2026-10-24，用户确认设计并要求开工（approved → implementing）。

## 元数据

- Change ID：`20261024-iam-authorization-audit-readable`
- 领域：`iam`
- 负责人：jy
- 创建日期：2026-10-24
- 目标发布日期：TBD
- [前端配套归档](../../../../../../ingot-admin/specs/changes/archive/2026/20261024-base-authorization-audit-readable/README.md)

## 目标

授权审计是只追加、不清理的数据，租户与平台的记录会持续增长。现有页码分页每次都执行 `COUNT(*)`，翻页越深越慢，按操作者或目标筛选时没有可用索引，而且新审计写入会让翻页结果错位。另外，列表的操作者、目标和变更值直接显示 ID，变更摘要只写“涉及哪些字段”，看不出具体改了什么。

本次改为基于 `(occurred_at, id)` 的游标分页，并补充筛选索引；接口按当前查看人的权限，一次批量解析审计中涉及的对象名称，让前端展示完整、可读的变更内容。

## 范围

包含：

- 平台、租户两个 `GET /v1/{platform|tenant}/authorization/audits` 改为游标分页，不再返回总数。
- 新增 `iam_authorization_audit` 的两条复合索引，并提供增量迁移脚本和回滚脚本。
- 响应新增页级 `references`，按查看人权限解析操作者、目标、关联分配/委派以及变更值中出现的对象名称。
- 修正之后新写入的审计：批量任务的 NAME 不再写类型编码，平台委派的接收对象不再写占位文本。

不包含：

- 审计表结构、写入时机、权限码、保留期限和归档策略。
- 跨域或跨租户查询、审计导出、安全事件查询。
- 回填历史审计数据（历史记录按“兼容读取”规则展示）。

## 关联变更

- 部分替代 [IAM 主变更 · 授权审计收敛](../20260912-iam-identity-access-management/AUTHORIZATION-AUDIT-REFINEMENT.md) 的页码分页及仅展示对象 ID 的约定。筛选参数、域边界、时间半开区间和字符串差异值契约继续沿用。

## 工件

- [需求](./REQUIREMENTS.md)
- [设计](./DESIGN.md)
- [任务](./TASKS.md)

## 完成记录

- 2026-11-07 用户确认平台及组织端的浏览器人工验收通过。同日完成 15 万条合成数据的 MySQL 8.4 执行计划验证，详见 [TASKS](TASKS.md#验证任务)。
- 实现提交为后端 `9719d178`、前端 `0776295`。
- 更新[平台能力](../../../../current/iam/platform-capabilities/README.md)及[数据库持久化](../../../../current/iam/database-persistence/README.md)。
- 游标查询和引用解析由独立 `AuthorizationAuditService` 编排。角色发布审计的 `ROLE_REVISION` 变更前值统一使用版本 ID，与变更后值一致。
