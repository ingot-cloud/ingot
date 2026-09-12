# PMS 数据授权与运行时快照

> 能力域：`pms` / `data-authorization`

## 摘要

按**资源 + 操作**配置数据范围，并在请求期用服务端授权快照执行行过滤与写归属校验。功能准入仍由 `@AdminOrHasAnyAuthority` / `@HasAnyAuthority` 判定；`@DataScope` 只按 `(resource, permission)` 取规则改 SQL。快照绝对新鲜度不超过 30 秒，不启用 LKG/地板放行。默认 SQL 谓词为已展开部门 `IN` 与本人 `OR` 部门；超大规模谓词见独立 active change。

## 边界

- **含**：资源目录、角色 data-rules、部门角色绑定去重、授权快照、分层缓存（无 Resilient/LKG）、`@DataScope` / `DataScopeGuard`、默认 SQL 谓词、示例闭环。
- **不含**：给全部 PMS 管理表注册行过滤；角色继承与显式拒绝；字段级脱敏；把超大 `IN` / `OR` 换成闭包表或 `UNION ALL`（见 [`20260912-mybatis-data-scope-predicate-scale`](../../../changes/active/20260912-mybatis-data-scope-predicate-scale/)）。
- 应用、菜单、功能权限模型见 [application-authorization](../application-authorization/README.md)。

## 所有者

- 策略与快照组装：`ingot-pms`
- 运行时过滤：`ingot-framework/ingot-data/ingot-data-mybatis-scope`、`ingot-data-mybatis`

## 关联模块

| 职责 | 路径 |
|---|---|
| 快照组装 | `AuthorizationSnapshotAssembler` |
| 内部快照 RPC | `InnerAuthorizationAPI`、`RemotePmsAuthorizationService` |
| 分层缓存与过滤器 | `InDataScopeConfig`、`AuthorizationSnapshotFilter` |
| 行过滤 | `DataScopeAOP`、`CustomDataPermissionHandler` |
| 写校验 | `DataScopeGuard` |
| 资源 CRUD | `PlatformApplicationAPI` `/apps/{appId}/resources` |
| 角色规则 | `PlatformRoleAPI`、`OrgRoleAPI` 的 `/data-rules` |
| 示例 | `ingot-test` 订单 / 公告 |

## 文档索引

- [SPEC](./SPEC.md)：规则模型、快照、注解、SQL 谓词、缓存与失败语义
- 关联能力：[application-authorization](../application-authorization/README.md)、[session-safety](../../security/session-safety/README.md)、[layered-cache](../../framework/layered-cache/README.md)
- 来源变更：`specs/changes/archive/2026/20260910-pms-rbac-data-authorization/`
