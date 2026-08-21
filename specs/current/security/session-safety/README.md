# 会话安全（在线会话 / 强制下线 / 并发策略）

> 能力域：`security` / `session-safety`

## 摘要

L5 已验收闭环：登录态从 jti 级 OnlineToken 索引收口为 **sid 会话模型**，撤销同时失效 Access Token 会话索引与 Refresh Token，Resource Server 以 Redis 为准判断在线，管理面归安全中心，并发策略可走中心或 Nacos 降级。

1. **会话主键**：`sid = OAuth2Authorization.id`，JWT 携带 `sid`；refresh 复用同一 sid，不新建会话。
2. **撤销彻底**：`SessionRevocationService.revokeBySid` 删除 Authorization（级联清会话主数据），堵住「踢下线后 refresh 复活」。
3. **在线判断**：无 sid / sid 在 Redis 不存在 → 拒绝；Redis 故障仅 30s 宽限；禁止 JWT-only 长期放行。
4. **执行面 vs 中心面**：Auth 持有 Redis 会话与 OAuth2 授权，只暴露 Inner `/inner/session/**`；安全中心 Platform 查询 / 强制下线；用户自助登出由 BFF 编排后 Feign 调 Auth。
5. **并发策略**：表 `session_concurrency_policy` + Auth `ingot.security.session.mode=local|remote`；remote 走 `Feign → LKG → Nacos 地板`，地板关且无 LKG 时 fail-closed 拒绝新登录。

## 边界

- **含**：sid 签发与 refresh 复用、Redis 会话 schema（含 TTL / 墓碑清理）、RS 校验、Auth Inner 查询与撤销、Platform 会话管理、账号域改密/锁定/禁用联动撤会话、`SESSION_REVOKED` / `SESSION_CONCURRENT_KICKOUT` 事件、并发策略 CRUD 与登录时执行、Auth 拆 `ingot-auth-api` / `ingot-auth-provider`、网关摘 `/auth/token/**`。
- **不含**：登录设备（需求 5.2）、登录地点解析、会话历史落库、MFA / 风险规则触发下线、交互式选择踢哪一个会话、管理员路径清除 BFF 会话键、`DevClientAPI` / `/auth/client/**` 迁移、JWT 权限列表从 `OnlineToken` 拆出（见 [R-2026-021 开工约束](../../../../docs/requirements/ROADMAP.md#r-2026-021-开工约束写-spec-前必读)）。

## 所有者

- 会话签发 / 存储 / 撤销执行面：`ingot-framework/ingot-security/ingot-security-authorization-server`、`ingot-framework/ingot-security/ingot-security-common`
- Auth Inner 与 Feign 契约：`ingot-service/ingot-auth/ingot-auth-provider`、`ingot-service/ingot-auth/ingot-auth-api`
- 中心管理面与并发策略表：`ingot-service/ingot-security/ingot-security-provider`
- 账号联动：`ingot-framework/ingot-security/ingot-security-account`
- 自助登出编排：`ingot-service/ingot-bff`
- RS / 网关校验：`ingot-framework/ingot-security`、`ingot-service/ingot-gateway`

## 关联模块

| 职责 | 路径 |
|---|---|
| sid claim / Redis 前缀 | `InJwtClaimNames.SID`、`RedisKeyConstants.OnlineToken` |
| 会话主数据读写 | `RedisOnlineTokenService` |
| 撤销收口 | `DefaultSessionRevocationService` |
| RS 校验与身份补全 | `InTokenAuthFilter`、`JwtInUserConverter` |
| 并发执行 | `SessionConcurrencyEnforcer`、`SessionConcurrencyPolicyResolver` |
| Platform 会话 | `PlatformSessionAPI`（`/platform/security/sessions`） |
| 并发策略 CRUD | `SessionConcurrencyPolicyAPI` |
| 前端对接（归档） | `specs/changes/archive/2026/20260817-security-session-safety/PLATFORM-API.md` |
| 存量键清理 | `bin/session_keys_purge.sh` |

## 文档索引

- [SPEC](./SPEC.md)：会话模型、Redis schema、接口、并发策略、配置、已知限制
- 来源变更：`specs/changes/archive/2026/20260817-security-session-safety/`
