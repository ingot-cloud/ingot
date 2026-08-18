# Phase 03 · 中心管理面

> 状态：pending  
> 依赖：Phase 02 退出条件满足

## 目标

会话查询与管理员强制下线的**对外管理面**迁到安全中心；Auth 不再为管理页反向依赖 PMS。本 Phase **不**交付并发策略 CRUD（Phase 04）。

## 实现要点

- Provider：`SessionAdminService` + `PlatformSessionAPI`（`/platform/security/session`）。
- 权限：`platform:security:session:query`、`platform:security:session:revoke`；`@AdminOrHasAnyAuthority`。
- 流程：Platform → `RemoteSessionService`（Auth Inner）→ 补 PMS 用户/租户名称。
- PMS 失败：名称空，sid 级字段仍返回（DESIGN 失败表）。
- 交付 [PLATFORM-API.md](../PLATFORM-API.md)（本 Phase 编写，步骤 A 不预建），与 Swagger 一致。
- 权限码若需入库：migration 或权限种子（实施时按 PMS 权限模型补，可与 014 分开以免和策略表耦合）。
- Auth `BizUserTokenService`：停止 Feign PMS；`GET /token/tokens` 改为 deprecated 包装或 301 文档说明；实现转调 `SessionRevocationService` / 内部查询以免兼容期行为倒退（D17）。
- 不把 Auth Redis 暴露给安全中心直连。

## 退出条件

- [ ] 管理员仅通过安全中心即可完成列表、详情、按 sid 下线、按用户下线。
- [ ] 列表含用户名/租户名（PMS 可用时）。
- [ ] Auth 不再为管理查询依赖 PMS。
- [ ] 不部署安全中心时登录/登出不受影响（A9）。
- [ ] PLATFORM-API 与代码字段一致。

## 回滚

保留 Auth 旧 `/token/*` 兼容实现；下线 Platform Controller 即可。Feign 契约可留。
