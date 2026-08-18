# Phase 03 · 中心管理面

> 状态：pending  
> 依赖：Phase 02 退出条件满足

## 目标

会话查询与管理员强制下线的**对外管理面**迁到安全中心。Auth Inner 与网关 `/auth/token/**` 摘除已在 Phase 02 完成；本 Phase 只补 Platform。本 Phase **不**交付并发策略 CRUD（Phase 04）。

## 实现要点

- Provider：`SessionAdminService` + `PlatformSessionAPI`（`/platform/security/session`）。
- 权限：`platform:security:session:query`、`platform:security:session:revoke`；`@AdminOrHasAnyAuthority`。
- 流程：Platform → `RemoteAuthSessionService`（Auth Inner）→ 补 PMS 用户/租户名称。
- PMS 失败：名称空，sid 级字段仍返回（DESIGN 失败表）。
- 交付 [PLATFORM-API.md](../PLATFORM-API.md)（本 Phase 编写，步骤 A 不预建），与 Swagger 一致。
- 权限码若需入库：migration 或权限种子（实施时按 PMS 权限模型补，可与 014 分开以免和策略表耦合）。
- 不把 Auth Redis 暴露给安全中心直连。
- `/auth/client/**` 保留，`DevClientAPI` 本期不迁移（D21）。

## 退出条件

- [ ] 管理员仅通过安全中心即可完成列表、详情、按 sid 下线、按用户下线。
- [ ] 列表含用户名/租户名（PMS 可用时）。
- [ ] 不部署安全中心时登录/登出不受影响（A9）。
- [ ] PLATFORM-API 与代码字段一致。

## 回滚

下线 Platform Controller 即可。Feign 契约与 Auth Inner 保留。不恢复 `TokenEndpoint`。
