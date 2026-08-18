# 会话安全（L5）

> 状态：draft

## 元数据

| 项 | 值 |
|---|---|
| Change ID | `20260817-security-session-safety` |
| 领域 | `security` |
| 负责人 | jy |
| 创建日期 | 2026-08-17 |
| 目标发布日期 | TBD |
| Roadmap | [安全中心分阶段落地 Roadmap](../../../../docs/requirements/themes/security-center-roadmap.md) · L5 |
| 需求来源 | [安全中心服务需求 §五 5.1 / 5.3 / 5.4](../../../../docs/requirements/themes/security-center-service.md) |
| 平台路线 | 建议编号 `R-2026-028`（见 [ROADMAP.md](../../../../docs/requirements/ROADMAP.md) Next） |

## 目标

把「登录之后的登录态」从当前的 **jti 级 OnlineToken 索引** 收口为可验收的会话安全闭环：

1. **会话模型**：JWT 携带 `sid`，`sid = OAuth2Authorization.id`；refresh 换发不新建会话。
2. **撤销彻底**：强制下线同时撤销 Access Token 会话索引 **与** Refresh Token（`oauth2:auth:*` / `oauth2:token:*`），堵住「踢下线后 refresh 复活」。
3. **在线判断以 Redis 为准**：Resource Server 不再在 OnlineToken miss 时 fallback 到纯 JWT claims 放行。
4. **管理面归安全中心**：Platform API 查询 / 强制下线；auth 只保留执行面 Inner API 与用户自助登出。
5. **并发会话策略可降级**：`remote → LKG → Nacos 地板`，替代 client `tokenSettings` 上的 `STANDARD/UNIQUE` 二值。

**核心原则**（承接 Roadmap 横切约定）：

| 原则 | 含义 |
|------|------|
| 执行面 vs 中心面 | Auth 持有 Redis 会话与 OAuth2Authorization，负责签发 / 续期 / 撤销；安全中心提供统一管理页面与并发策略 CRUD |
| 策略 vs 运行时分离 | 可 Nacos 降级的是**并发策略参数**；在线会话状态永远在 Redis，不可「降级为空」 |
| 统一管理不可降级 | 不部署安全中心时失去 Platform 查询与跨应用治理；Redis 会话、自助登出、基础强制下线仍可用 |
| 主链路不阻塞 | 安全事件上报失败不回滚登出 / 下线；PMS 展示信息拼装失败不阻断会话列表的 sid 级数据 |

## 依赖

| 前置 | 说明 |
|------|------|
| L3 统一安全事件中心 | 下线原因入 `security_event`；复用 `SecurityEventPublisher` |
| L4 访问防护 | Platform / Inner / Feign 三分法与 `SecurityPolicyDomain` 失效广播范式 |
| `ingot-cache` 分层缓存 | [20260730-framework-layered-cache](../20260730-framework-layered-cache/README.md) Phase 01/02 已完成；Phase 04 并发策略直接用 `LayeredCacheBuilder`（见 D4） |
| 现网会话基线 | `RedisOnlineTokenService` + `RedisOAuth2AuthorizationService` + `TokenEndpoint` |

## 范围

**包含：**

- `sid` claim 签发、refresh 复用、`OnlineToken` 升为会话主数据（主键 sid）。
- Redis 会话 schema 迁移与灰度双读；撤销走 `oAuth2AuthorizationService.remove(findById(sid))`。
- Resource Server / 网关按 sid 做在线校验；去掉 JWT-only fallback。
- Auth Inner API `/inner/session/**`；用户自助 `DELETE /token` 改为按 sid 撤销。
- 安全中心 Platform API `/platform/security/session/**` + `RemoteSessionService` Feign；用户/租户展示由中心拼 PMS。
- 账号域联动：密码修改 / 重置、账号锁定、账号禁用 → 撤销该用户会话。
- 安全事件：`SESSION_REVOKED`、`SESSION_CONCURRENT_KICKOUT`（用户自助登出沿用已有 `LOGOUT`）。
- 并发策略表 `session_concurrency_policy`、Nacos 地板、`ingot.security.session.mode=local\|remote`。
- `cleanAllExpiredOnlineUsers()` 去掉 `KEYS` 扫描。
- 验收后新建 `specs/current/security/session-safety/`。

**不包含（非目标）：**

- **5.2 登录设备**：可信设备、陌生设备提醒、设备禁用（Roadmap 阶段二）。本闭环只展示已采集的 IP / UA / deviceType / OS / browser。
- **登录地点解析**：`OnlineToken.location` 仍可为空，不接 IP 库。
- **会话历史落库**：权威在线态在 Redis；历史追溯依赖 L3 `security_event`，不新建 `security_session` 表。
- **MFA / 二次确认 / 风险规则触发下线**（Roadmap 阶段三）。
- **按设备撤销**（依赖 5.2 设备标识）。
- **「由用户选择踢出哪一个」** 的交互式并发超限（P2，本闭环超限行为仅拒绝新登录 / 踢最旧 / 踢全部）。
- **跨节点主动失效 BFF Cookie 会话**（pub/sub 清 `in:bff_session:*`）；本闭环 BFF 仅冗余存 sid，强制下线后下次请求由 RS 401。
- **R-2026-021 Token 与会话优化**（Redis 压力 / 查询性能专项，Later 独立项）。

## 工件

- [需求](./REQUIREMENTS.md)
- [设计](./DESIGN.md)
- [任务](./TASKS.md)
- [Phase 01 会话模型与撤销收口](./phases/01-session-model.md)
- [Phase 02 执行面与事件](./phases/02-execution-events.md)
- [Phase 03 中心管理面](./phases/03-center-admin.md)
- [Phase 04 并发会话策略](./phases/04-concurrency-policy.md)

## 待审阅决策（D1–D5）

| ID | 议题 | 推荐决议 | 状态 |
|----|------|----------|------|
| D1 | Redis 不可用时 Resource Server 姿态 | **区分键不存在与 Redis 故障**：键不存在 → 拒绝（下线生效）；Redis 异常 → 短窗口（建议 30s）允许签名仍有效的 JWT 通过，打点 `session.redis.unavailable` 并告警，**禁止**把 JWT-only 作为长期 fallback | 待确认 |
| D2 | 无 sid 的旧 JWT 兼容窗口 | 窗口 = `max(accessToken TTL, 3600s)` + 一次滚动发布；窗口内无 sid 回退 `token:jti:{jti}`；窗口结束后无 sid → 拒绝 | 待确认 |
| D3 | 事件类型与 `20260812` SoT 清理的时序 | 会话事件只改 **api 枚举** + `DefaultPriorityClassifier`；**不**双写 account-core（生产者在 Auth，不在 account-core）。若 20260812 先合入，改 codes 模块常量 | 待确认 |
| D4 | 并发策略降级链实现 | **直接用 `LayeredCacheBuilder`**，不手写 `ResilientLoginFailurePolicyLoader` 同类代码 | 待确认 |
| D5 | BFF `sessionId` 与 auth `sid` | `BffSession` **冗余存 sid**；logout 按 sid 调 Auth；不在本闭环做 BFF Redis 的跨节点主动失效 | 待确认 |

审阅通过后把上表「状态」改为已决议，change 状态转 `approved`，再进入实施。

## 完成记录

- 完成日期：
- 关联提交或 PR：
- 更新的 current capability：`specs/current/security/session-safety/`（验收后新建）
- 与原设计的差异：
- 取消原因：

## 后续跟踪（拆出为新 change）

1. **登录设备（需求 5.2）**：设备标识、可信/陌生、用户自助移除、管理员禁用。
2. **登录地点**：IP 库解析 `location`。
3. **BFF Cookie 会话主动失效**：按 sid 广播删除 `in:bff_session:*`。
4. **交互式并发超限**：超限时让用户选择踢哪一个会话。
5. **按租户 / 应用批量下线的运维大盘**（本闭环 Inner API 可先留口，Platform 完整交互后置）。
