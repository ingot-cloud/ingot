# 多租户 RBAC、菜单解耦与数据权限

> 状态：completed

## 元数据

- Change ID：`20260910-pms-rbac-data-authorization`
- 领域：PMS / Security / MyBatis
- 负责人：jy
- 创建日期：2026-09-10
- 发布安排：一次性同步切换
- 当前阶段：T0–T9 已落地并写入 current。

## 目标

保留 SaaS 应用授权、平台预设角色与租户追加授权，统一菜单、按钮、API 和资源数据范围的授权事实来源。权限编码独立于菜单路由；部门角色按实际绑定部门生效；在线撤权最多 30 秒生效。

## 范围

- 包含 PMS 数据模型及管理接口、安全框架授权解析与缓存、MyBatis 数据权限、示例和一次性迁移。
- 不批量为 PMS 用户、部门、角色管理接口增加行级过滤；独立前端仓库的实现不在本仓库修改范围，但客户端契约联调是发布门禁。
- 超大 `IN` / `OR` 的谓词升级不在本 change 交付，见 [`20260912-mybatis-data-scope-predicate-scale`](../../active/20260912-mybatis-data-scope-predicate-scale/)（draft）。
- 不引入角色继承、租户拒绝平台默认权限、复杂布尔权限表达式、租户自定义业务应用设计器。
- 本变更仅有一个对外发布单元。实施批次和阶段验收写入 TASKS，不建立独立发布的 phases 工件。

## 工件

- [需求与验收](./REQUIREMENTS.md)
- [设计与迁移](./DESIGN.md)
- [决策、任务及门禁](./TASKS.md)
- [前端联调接口](./FRONTEND.md)

## 相关基线与事实差异

- [应用授权基线](../../../current/pms/application-authorization/SPEC.md)
- [数据授权基线](../../../current/pms/data-authorization/SPEC.md)
- [会话安全基线](../../../current/security/session-safety/SPEC.md)
- [分层缓存基线](../../../current/framework/layered-cache/SPEC.md)
- 后续规模谓词：[`20260912-mybatis-data-scope-predicate-scale`](../../active/20260912-mybatis-data-scope-predicate-scale/)，状态 draft。

## 审批及完成记录

- 审批：2026-09-10 用户确认开始实施，状态转为 implementing。
- 完成日期：2026-09-12
- 关联提交或 PR：`cd33a640`
- 更新的 current capability：`pms/application-authorization`、`pms/data-authorization`、`security/session-safety`、`framework/layered-cache`
- 与原设计的差异：
  - 超大 `IN` / `OR` 谓词已拆至独立 active change，本基线只记录默认 `IN` / `OR`。
  - 租户覆盖到期判定为 `now.isAfter(validUntil)`，等于截止时刻仍可用。
  - 授权热缓存只有用户快照键，未做角色聚合键与 `VersionedDerivedCache`；失效为事务后本节点 `evictAll` 再广播全量清理。
  - `PermissionMatcher` 仍识别遗留 `:*`；`platform_app.menu_id` 未 DROP。
  - 登录会话 `OnlineToken.authorities` 仅角色码，业务权限由 `AuthorizationSnapshotFilter` 合并。
- 取消原因：—
