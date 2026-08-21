# Phase 03 · 中心管理面

> 状态：implementing（编码完成，E2E 待联调环境）  
> 依赖：Phase 02 退出条件满足

## 目标

会话查询与管理员强制下线的**对外管理面**迁到安全中心。Auth Inner 与网关 `/auth/token/**` 摘除已在 Phase 02 完成；本 Phase 只补 Platform。本 Phase **不**交付并发策略 CRUD（Phase 04）。

## 实现要点

- Provider：`SessionAdminService` + `PlatformSessionAPI`（`/platform/security/sessions`，路径按资源名复数收敛，见 DESIGN 修订说明）。
- 权限：`platform:security:session:query`、`platform:security:session:revoke`；`@AdminOrHasAnyAuthority`。
- 流程：Platform → `RemoteAuthSessionService`（Auth Inner）→ 补 PMS 用户/租户名称。
- PMS 失败：名称空，sid 级字段仍返回（DESIGN 失败表）。Auth 不可达则抛错给管理员，不静默返回空列表。
- 查询范围：`clientId` 与 `userId` 至少给一个；只给 `userId` 时跨 Client 查询后内存分页。
- 撤销原因固定 `ADMIN_REVOKE`、操作者取当前登录管理员，前端不可传。
- 交付 [PLATFORM-API.md](../PLATFORM-API.md)（本 Phase 编写，步骤 A 不预建），与 Swagger 一致。
- 权限码入库：`014_session_admin_permission_seed.sql`（操作级 API 权限挂既有「在线用户」菜单权限下），与 Phase 04 策略表迁移分开。
- 不把 Auth Redis 暴露给安全中心直连。
- `/auth/client/**` 保留，`DevClientAPI` 本期不迁移（D21）；前端 Client 选择器数据源即 `GET /auth/client/page`。

## 退出条件

- [ ] 管理员仅通过安全中心即可完成列表、详情、按 sid 下线、按用户下线。
  - 四个端点已实现并有单测覆盖编排逻辑；端到端需联调环境
- [ ] 列表含用户名/租户名（PMS 可用时）。
  - `SessionAdminServiceImplTest` 覆盖 PMS 可用（补名）与不可用（名称空、sid 级字段保留）两条路径
- [x] 不部署安全中心时登录/登出不受影响（A9）。
  - 依赖方向单向：中心 → Auth Inner；Auth / BFF / 网关无任何对安全中心的会话依赖，全仓检索无反向引用
- [x] PLATFORM-API 与代码字段一致。
  - 字段表逐项对照 `PlatformSessionVO`；枚举值按 `TokenAuthTypeEnum` / `UserTypeEnum` 的序列化值（`0`/`1`）而非枚举名书写

## 回滚

下线 Platform Controller 即可。Feign 契约与 Auth Inner 保留。不恢复 `TokenEndpoint`。
