# Phase 02 · 执行面与事件

> 状态：implementing（编码完成，集成验收待联调环境）  
> 依赖：Phase 01 退出条件满足（含 `ingot-auth-api` 已可用）

## 目标

Auth 暴露 Inner 会话查询/撤销 API，供安全中心、账号域与 BFF 调用；删除 `TokenEndpoint` 与网关 `/auth/token/**`；强制下线与并发踢人写入 L3 事件；密码修改/重置、锁定、禁用联动撤会话。本 Phase **不**做 Platform 页面。

## 实现要点

- `ingot-auth-provider`：`InnerSessionAPI`（`/inner/session`），`@Permit(INNER)`；查询只返回 `OnlineToken` 运行时字段。
- 用户集合按 IP 过滤依赖 Phase 01 的 `session:ip:` 索引。
- `ingot-auth-api`：新增 `RemoteAuthSessionService`，`@FeignClient(value = ServiceNameConstants.AUTH_SERVICE)`（D20）。从 `RemoteAuthTokenService` 去掉 `/token` revoke，该模块只保留 `/oauth2/**` 协议契约。
- 三个消费方共用 `RemoteAuthSessionService`：安全中心 provider、账号域 adapter、BFF（及将来 App BFF）。账号域只依赖 `ingot-auth-api`，**不**依赖 `ingot-security-api`。
- BFF logout 切到 `RemoteAuthSessionService.revokeBySid(session.sid)`（A16）；`sid` 为空只清自己的键，**不**回落已删除的旧 API（D17）。
- **删除** `TokenEndpoint`、`BizUserTokenService`（D8 / D17）。无 deprecated 包装。
- 网关三套 `in-service-auth` predicate 改为仅 `/auth/client/**`；顺带删除 `ingot.vc.verifyUrls` 中不在任何 auth 路由内的 `/auth/oauth2/*`。
- Inner 的对外安全性依赖既有机制：`OAuth2InnerResourceFilter` 校验 `In-Inner-From: Inside`，该头由网关入口剥离。Feign 不经网关。
- 撤销链路一律 `调用方 → Auth Inner`，Auth 不回调任何服务（D19）。
- `SecurityEventType`：`SESSION_REVOKED`、`SESSION_CONCURRENT_KICKOUT`（AUTH）。
- `DefaultPriorityClassifier`：上述两类 DURABLE。若 [20260812-security-event-type-sot-cleanup](../../../active/20260812-security-event-type-sot-cleanup/README.md) 已合入，改 codes 模块而非硬编码重复（D3）。
- **不**修改 account-core 重复枚举（会话事件不由 account-core 生产）。
- `SessionRevocationService` 在成功删除后发布事件；`reason`/`actor`/`sid` 写入 payload；`session_id` 列填 sid。
- 用户自助登出发 `LOGOUT`，不发 `SESSION_REVOKED`。
- 联动点：`ChangePassword` / `ResetPassword` / `LockAccount` / `disableAccount` 成功后 `revokeByUser(..., clientId=null)`。
- 联动失败：不回滚账号事务；`session.revoke.linkage.failure` 计数 + ERROR 日志。

## 退出条件

- [x] Inner 可按 tenantId+clientId 分页、按 sid 详情、按 user、按 IP 查询。（`BizSessionServiceImplTest` 覆盖四条查询路径与越界过滤）
- [x] Inner DELETE 与 Phase 01 撤销语义一致（RT 失效）。（委托 `SessionRevocationService`，`InnerSessionAPITest` 断言原因与 actor 透传）
- [ ] 密码/锁定/禁用后该用户当前租户全部会话 RS 401。（单测覆盖到 Port 调用与 Feign 入参；端到端待联调）
- [ ] BFF 在 Access Token 已过期时仍能按 `BffSession.sid` 调 Inner 撤销成功（A16）。（`BffAuthServiceLogoutTest` 覆盖有/无 sid 与 Auth 故障；实例验证待联调）
- [x] `TokenEndpoint` 已删除；网关 `/auth/token/**` 返回 404；`RemoteAuthTokenService` 无 `/token`（A13 / A19）。（三套网关 predicate 仅 `/auth/client/**`）
- [ ] `security_event` 出现 `SESSION_REVOKED` 或 `SESSION_CONCURRENT_KICKOUT`，含 sid。（Listener 与 classifier 已就绪并有单测；落库需安全中心联调）
- [x] 停安全中心时 Inner 与联动仍可用（S11）。（事件发布与联动均 fail-open，撤销不因上报失败回滚）
- [x] `AUTH_SERVICE` 的 `@FeignClient` 定义全仓唯一，位于 `ingot-auth-api`（A18）。（全仓检索仅 `RemoteAuthTokenService` / `RemoteAuthSessionService`）

## 回滚

下线 Inner 路由与 Feign；联动调用加开关 `ingot.security.session.linkage-revoke-enabled`（默认 true）以便紧急关闭。事件类型保留无害。
