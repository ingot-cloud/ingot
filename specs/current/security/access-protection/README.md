# 访问防护（网关执行面 + 登录失败保护）

> 能力域：`security` / `access-protection`

## 摘要

L4 已验收闭环，覆盖三块能力：

1. **网关策略执行面收口**：Nacos 三环境启用 `ingot-gateway-rule-client` 限流 / 黑白名单 / 违规升级（`policy.mode=remote`）；Sentinel 为唯一网关限流引擎；migration `011` 种子规则（登录路径 + 业务 API 基线）；移除路由级 `RequestRateLimiter`。
2. **网关策略 remote 弹性**：共享快照 `SecurityPolicySnapshotVO` 走统一分层缓存（`ingot-cache`：`L1 → L2 → remote → LKG → Nacos 地板`），fail-closed；Actuator `securitypolicy` / `layeredcache` 可观测降级来源。
3. **四维度登录失败保护**：Auth 侧 `ingot-security-access` 按 IP / 设备 / Client / 账号+IP 计数，达阈值写 Redis 临时封禁（与 Gateway `TempBlockStore` 共用 `in:gw:bl:tmp:*`）；策略 Platform CRUD + Inner Feign + Auth `mode=remote` 与 `remote → LKG → Nacos 地板` 降级链。

与 L2 **账号 lockout**、L3 **安全事件中心** 分工明确：账号锁定仍仅账号维度；新封禁/违规审计写入 `security_event`（`gateway_blacklist_event` 历史只读）。

## 边界

- **含**：网关四域 SDK remote 执行、违规升级、登录失败保护策略与执行、共享快照 LKG/地板、ACCESS 类扩展事件（`LOGIN_FAIL_*` / 既有 `RATE_LIMIT_VIOLATION`）。
- **不含**：账号 lockout remote 中心化、L6 挑战 SDK 启用、行为型防爆破（同 IP 多账号）、Platform 安全事件读侧 / 封禁审计新写入 `gateway_blacklist_event`、HTTP Method 参与 Sentinel 编译。

## 所有者

- 网关策略 SDK：`ingot-framework/ingot-gateway-rule-client`、`ingot-framework/ingot-cache`
- 网关执行面：`ingot-service/ingot-gateway`
- 登录失败保护：`ingot-framework/ingot-security/ingot-security-access-{core,adapter}`
- 策略管理面：`ingot-service/ingot-security/ingot-security-provider`

## 关联模块

| 职责 | 路径 |
|---|---|
| 共享快照分层缓存 | `ingot-cache`、`GatewayRuleClientAutoConfiguration` |
| Sentinel 规则编译 | `SentinelGatewayConfiguration` |
| 临时封禁 / 违规计数 | `TempBlockStore`、`ViolationCounter`、`BlacklistFilter` |
| 登录失败计数与封禁 | `LoginFailureProtectionService`、`LoginFailureAccessListener` |
| 登录失败策略 Platform/Inner | `LoginFailureProtectionAPI`、`InnerLoginFailurePolicyAPI` |
| 运维文档 | `docs/modules/security-center/GATEWAY-RATE-LIMIT.md` |
| E2E | `test-case/security-policy-e2e.md` |

## 文档索引

- [SPEC](./SPEC.md)：配置、数据模型、Redis Key、Platform API、降级语义、已知限制
- Platform 前端对接（归档）：`specs/changes/archive/2026/20260729-security-access-protection/PLATFORM-API.md`
- 来源变更：`specs/changes/archive/2026/20260729-security-access-protection/`
