# Phase 02 · 执行面与事件

> 状态：pending  
> 依赖：Phase 01 退出条件满足

## 目标

Auth 暴露 Inner 会话查询/撤销 API，供安全中心与账号域调用；强制下线与并发踢人写入 L3 事件；密码修改/重置、锁定、禁用联动撤会话。本 Phase **不**做 Platform 页面。

## 实现要点

- `ingot-auth`：`InnerSessionAPI`（`/inner/session`），`@Permit(INNER)`；查询只返回 `OnlineToken` 运行时字段。
- 用户集合按 IP 过滤依赖 Phase 01 的 `session:ip:` 索引。
- `ingot-security-api`：`RemoteSessionService`，`@FeignClient` **value = Auth 服务名**（`ServiceNameConstants` 中 Auth 常量，不是 SECURITY_SERVICE）。
- 账号域（PMS/Member adapter）注入同一 Feign 或并行 `RemoteAuthSessionService` 别名，避免账号模块依赖过宽。
- `SecurityEventType`：`SESSION_REVOKED`、`SESSION_CONCURRENT_KICKOUT`（AUTH）。
- `DefaultPriorityClassifier`：上述两类 DURABLE。若 [20260812-security-event-type-sot-cleanup](../../20260812-security-event-type-sot-cleanup/README.md) 已合入，改 codes 模块而非硬编码重复（D3）。
- **不**修改 account-core 重复枚举（会话事件不由 account-core 生产）。
- `SessionRevocationService` 在成功删除后发布事件；`reason`/`actor`/`sid` 写入 payload；`session_id` 列填 sid。
- 用户自助登出发 `LOGOUT`，不发 `SESSION_REVOKED`。
- 联动点：`ChangePassword` / `ResetPassword` / `LockAccount` / `disableAccount` 成功后 `revokeByUser(..., clientId=null)`。
- 联动失败：不回滚账号事务；`session.revoke.linkage.failure` 计数 + ERROR 日志。

## 退出条件

- [ ] Inner 可按 tenantId+clientId 分页、按 sid 详情、按 user、按 IP 查询。
- [ ] Inner DELETE 与 Phase 01 撤销语义一致（RT 失效）。
- [ ] 密码/锁定/禁用后该用户当前租户全部会话 RS 401。
- [ ] `security_event` 出现 `SESSION_REVOKED` 或 `SESSION_CONCURRENT_KICKOUT`，含 sid。
- [ ] 停安全中心时 Inner 与联动仍可用（S11）。

## 回滚

下线 Inner 路由与 Feign；联动调用加开关 `ingot.security.session.linkage-revoke-enabled`（默认 true）以便紧急关闭。事件类型保留无害。
