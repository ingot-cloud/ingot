# 会话安全（L5）

> 状态：implementing（Phase 01–02 代码与单测完成，待联调环境执行集成验收）

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
4. **管理面归安全中心**：Platform API 查询 / 强制下线；用户自助登出由 BFF（及将来的 App BFF）编排后 Feign 调 Auth Inner；Auth 会话相关只留 `/inner/session/**`，网关仅因 D21 仍暴露 `/auth/client/**`（D8 / D20 / D21）。
5. **并发会话策略可降级**：`remote → LKG → Nacos 地板`，替代 client `tokenSettings` 上的 `STANDARD/UNIQUE` 二值。

**核心原则**（承接 Roadmap 横切约定）：

| 原则 | 含义 |
|------|------|
| 执行面 vs 中心面 | Auth 持有 Redis 会话与 OAuth2Authorization，负责签发 / 续期 / 撤销；安全中心提供统一管理页面与并发策略 CRUD |
| **依赖单向** | BFF / 安全中心 / 账号域 → Auth（Feign，不经网关）；Auth **不**反向调用它们，也不读写它们私有的 Redis schema（D19） |
| 策略 vs 运行时分离 | 可 Nacos 降级的是**并发策略参数**；在线会话状态永远在 Redis，不可「降级为空」 |
| 统一管理不可降级 | 不部署安全中心时失去 Platform 查询与跨应用治理；Redis 会话、BFF 自助登出、Auth Inner 强制下线仍可用 |
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
- Redis 会话 schema 切换（**不兼容旧 JWT**，见 D2）；撤销走 `oAuth2AuthorizationService.remove(findById(sid))`。
- `BffSession.sid`：BFF 自助登出改为按 sid 精确撤销，不再依赖 Access Token 反查授权记录（D5）。
- Resource Server / 网关按 sid 做在线校验；去掉 JWT-only fallback。
- Auth Inner API `/inner/session/**`（BFF 自助登出与安全中心签退共用）；删除 `TokenEndpoint` `/token/**`，无 deprecated 窗口（D8 / D17）。
- Auth 拆为 `ingot-auth-api` + `ingot-auth-provider`：api 模块承担 Feign RPC 契约（对齐 pms / member / security），BFF 自建 `AuthClient` 迁入后删除（D20）。
- 网关路由收敛：`in-service-auth` 摘除 `/auth/token/**`，保留 `/auth/client/**`（D21）。
- 安全中心 Platform API `/platform/security/session/**` + `ingot-auth-api` 的 `RemoteAuthSessionService` Feign；用户/租户展示由中心拼 PMS。
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
- **BFF 作为踢人入口**：BFF 不暴露管理侧撤销 API，也不承担会话列表（D5）。
- **非自助路径的 BFF 会话清理**：管理员下线 / 账号联动 / 并发踢旧后，`in:bff_session:{sessionId}` 不做跨服务清除，浏览器由一次 401 引导重登、残键随 TTL 消亡（D19 否决直删方案）。
- **`DevClientAPI` / `/auth/client/**` 迁移**：网关继续暴露该端点，本期不迁移（D21）。
- **旧 JWT / 旧 API 均不兼容**（D2 / D17）：无 sid 的存量 Token 上线即失效；`TokenEndpoint` 本 change 内删除，不保留 deprecated 包装。
- **R-2026-021 Token 与会话优化**（Redis 压力 / 查询性能专项，Later 独立项）。

## 工件

- [需求](./REQUIREMENTS.md)
- [设计](./DESIGN.md)
- [任务](./TASKS.md)
- [Phase 01 会话模型与撤销收口](./phases/01-session-model.md)
- [Phase 02 执行面与事件](./phases/02-execution-events.md)
- [Phase 03 中心管理面](./phases/03-center-admin.md)
- [Phase 04 并发会话策略](./phases/04-concurrency-policy.md)

## 待审阅决策（D1–D21）

D6–D18 全文见 [DESIGN 决策表](./DESIGN.md#关键设计决策)。下表含用户显式修订项；其余按推荐决议于 2026-08-18 一并闭合。

| ID | 议题 | 决议 | 状态 |
|----|------|------|------|
| D1 | Redis 不可用时 Resource Server 姿态 | **区分键不存在与 Redis 故障**：键不存在 → 拒绝；Redis 异常 → 短窗口（建议 30s）允许签名仍有效的 JWT 通过，打点 `session.redis.unavailable` 并告警，**禁止**把 JWT-only 作为长期 fallback | **已决议 2026-08-18**（按推荐） |
| D2 | 无 sid 的旧 JWT | **不做兼容**。上线即失效，用户重新登录 | **已决议 2026-08-18** |
| D3 | 事件类型与 `20260812` SoT 清理的时序 | 会话事件只改 **api 枚举** + `DefaultPriorityClassifier`；**不**双写 account-core。若 20260812 先合入，改 codes 模块常量 | **已决议 2026-08-18**（按推荐） |
| D4 | 并发策略降级链实现 | **直接用 `LayeredCacheBuilder`**，不手写同类 Resilient loader | **已决议 2026-08-18**（按推荐） |
| D5 | 撤销入口与 BFF 的分工 | **按调用来源分流**：① 用户自助登出由 **BFF 编排**（及将来的 App BFF），方向 BFF → Auth Inner；② 管理员下线 / 账号联动 / 并发踢旧由安全中心 / 账号域 / Auth 自身直调 Auth Inner，**不经 BFF** | **已决议 2026-08-18** |
| D8 | Auth 对外保留什么 | **会话相关只留 Inner RPC**。用户自助登出在边缘代理（当前 Web BFF，将来可有 App BFF）完成编排后 Feign 调 Auth `/inner/session/**`；管理员签退由安全中心 Feign 调同一 Inner。Auth **不**再提供 `/token/**`。因 D21，网关仍暴露 `/auth/client/**`；OAuth2 协议端点 `/oauth2/**` 由 BFF Feign 直连，不经网关 | **已决议 2026-08-18**（修订） |
| D17 | 旧 HTTP API | **不兼容**。`TokenEndpoint`（`DELETE /token`、`GET /token/tokens`、`DELETE /token/jti`、`DELETE /token/user`）本 change 内删除，无 deprecated 包装、无双路径。其它旧接口同样不留兼容窗口，统一按新契约改造 | **已决议 2026-08-18**（修订） |
| D19 | Auth 是否清除 BFF 会话键 | **否决**。残键靠 401 + TTL 收敛；不引入反向索引、跨服务写与循环依赖 | **已决议 2026-08-18（否决）** |
| D20 | Auth 是否拆 `ingot-auth-api` | **拆，并入本 change**。api 承担 Feign RPC，对齐其它服务 | **已决议 2026-08-18** |
| D21 | `/auth/client/**` 去向 | **保留网关暴露，本期不迁移** | **已决议 2026-08-18** |

D6、D7、D9–D16、D18 按 DESIGN 推荐决议闭合（`sid = authorizationId`、保留 `OnlineToken` 类名、中心不直连 Auth Redis、无 JWT 黑名单、lastAccessAt 不写热路径、IP 索引、注册表清理、`USER_CLIENT` 维度、client UNIQUE/STANDARD 映射、账号联动范围、租户级踢光 Inner 预留）。

审阅通过（2026-08-18）后 change 转 `approved`，随即进入 Phase 01 实施。

## 完成记录

- 完成日期：
- 关联提交或 PR：
- 更新的 current capability：`specs/current/security/session-safety/`（验收后新建）
- 与原设计的差异：
- 取消原因：

## 后续跟踪（拆出为新 change）

1. **登录设备（需求 5.2）**：设备标识、可信/陌生、用户自助移除、管理员禁用。
2. **登录地点**：IP 库解析 `location`。
3. **交互式并发超限**：超限时让用户选择踢哪一个会话。
4. **按租户 / 应用批量下线的运维大盘**（本闭环 Inner API 可先留口，Platform 完整交互后置）。
5. **BFF 会话的事件驱动清理（D19）**：L3 事件中心成熟后，BFF 订阅 `SESSION_REVOKED` 清理自己的会话键，实现「干净登出」体验。
