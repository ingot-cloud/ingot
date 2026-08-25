# 访问防护 SPEC

> 记录当前已验收并在线生效的系统事实（L4 As-Built）。

## 1. 网关策略 SDK

### 1.1 域开关与模式

各域独立：`ingot.security.<domain>.enabled` + `ingot.security.<domain>.policy.mode`（`local` | `remote`）。

| 域 | 前缀 | 生产默认 |
|---|---|---|
| 限流 | `ingot.security.ratelimit` | `enabled=true`, `mode=remote` |
| 黑白名单 | `ingot.security.blacklist` | `enabled=true`, `mode=remote` |
| 违规升级 | `ingot.security.violation-escalation` | `enabled=true`, `mode=remote` |
| 挑战 | `ingot.security.challenge` | L4 **未启用** SDK 执行面 |

`local` 模式：规则来自 Nacos yaml（`RateLimitProperties` 等），**不读 LKG**。  
`remote` 模式：Feign `GET /inner/security/policy/snapshot`；yaml 同前缀下 `groups`/`rules` **仅作 Nacos 地板**。

### 1.2 共享快照缓存（`ingot.security.policy.client`）

四域共用一份 `SecurityPolicySnapshotVO` 分层缓存（框架 `ingot-cache`）：

```text
刷新通知 → L1 Caffeine → L2 Redis → Resilient(remote → LKG → 地板)
```

| 配置 | 默认 | 说明 |
|---|---|---|
| `invalidation-enabled` | `true` | 订阅 `SecurityPolicyInvalidationEvent` |
| `resilience-enabled` | `true` | 启用 LKG + 地板阶梯 |
| `local-floor-enabled` | `true` | 无 LKG 时落 Nacos 地板 |
| `lkg-redis-key` | `in:sec:policy:lkg:snapshot` | LKG 长存 |
| `cache.l2-redis-key` | `in:sec:policy:snapshot` | L2 热缓存 |
| `cache.l1-ttl` | `5m` | L1 兜底 stale 窗口 |

**语义**：LKG 仅在远端成功时刷新；`evictAll` 只清 L1/L2，不清 LKG。  
Actuator：`GET /actuator/securitypolicy`（来源）、`GET /actuator/layeredcache`（实例列表）。

### 1.3 限流执行

- 编译：`SentinelGatewayConfiguration` → `GatewayRuleManager` / `GatewayApiDefinitionManager`。
- 维度：`IP` / `DEVICE` / `USER` / `CLIENT`（Header 见 `RateLimitDimension`）。
- 参数：`qps` → Sentinel `count`；`interval_sec` → `intervalSec`；`burst`；`control_behavior`（`F`/`Q`）。
- 未配置路径：**默认不限流**（白名单式）。
- 已知限制：`EndpointPattern.method` 不参与 Sentinel 编译。

### 1.4 违规升级

单行配置（Platform `gateway_violation_escalation` 或 local yaml）：

| 字段 | 默认 |
|---|---|
| `window_sec` | 60 |
| `block_threshold` | 30 |
| `temp_block_ttl_sec` | 900 |

窗口内限流 429 达阈值 → `TempBlockStore.tryBlockFirst`（Redis SETNX）写入临时封禁 → 后续请求 403。

**升级事件边沿**：仅首次占位成功时 `BlacklistEventReporter.report` 一条 `RATE_LIMIT_VIOLATION`；已存在则只刷新 TTL，不上报。同一 key 一次 temp-block 生命周期至多 1 条。去重在 `SentinelBlockHandler` 内完成（同波并发可能已越过 `BlacklistFilter`）。

### 1.5 Nacos 地板

`in-security-policy.yml`（gateway `spring.config.import`）仅承载**地板数据**（各域 `policy.groups` / `rules` 等），**不声明** `enabled` / `mode`（避免与 `in-service-gateway.yml` 冲突）。

## 2. 登录失败保护（Auth）

### 2.1 配置

前缀：`ingot.security.access`（**配在 `in-service-auth.yml`**，非 Gateway）。

| 键 | 默认 |
|---|---|
| `mode` | `local`（代码缺省；生产 Nacos 为 `remote`） |
| `policy.fallback.local-floor-enabled` | `true` |
| `policy.cache.l1-enabled` | `true` |
| `policy.cache.l1-ttl` | `5m` |
| `policy.cache.l2-enabled` | `true` |
| `policy.cache.l2-ttl` | `30m` |
| `policy.cache.l2-redis-key` | `in:sec:lf:policy:snapshot` |

`mode=local`：读 Nacos `login-failure.*`；`mode=remote`：Feign 拉安全中心四维策略，走 `ingot-cache` 分层链 `L1 → L2 → remote → LKG → 地板`。失效广播调用 `evictAll()` 会真实清除 L1/L2。

LKG Redis：`in:sec:lf:policy:lkg`（不随 evict 清除）。Actuator：`GET /actuator/loginfailurepolicy`（若启用）。

### 2.2 策略表

`ingot_security.login_failure_protection_policy`（migration `011`），每维度一行：

| dimension | 默认 max / window / TTL |
|---|---|
| `IP` | 50 / 1min / 3600s |
| `DEVICE` | 30 / 5min / 1800s |
| `CLIENT` | 100 / 5min / 3600s |
| `ACCOUNT_IP` | 10 / 5min / 3600s |

Platform：`/platform/security/access/login-failure-policies`（CRUD）；变更发 `SecurityPolicyDomain.LOGIN_FAILURE_PROTECTION` 失效。

### 2.3 运行时 Redis

| 用途 | Key |
|---|---|
| 失败计数 | `in:sec:lf:{dimension}:...` |
| 临时封禁（与网关共用） | `in:gw:bl:tmp:{keyType}:{keyValue}` |

达阈值：**仅临时封禁**，不触发 L2 账号 lockout（D3）。  
`invalid_client` **不计入** Client 维度（D10）。  
超阈值 DURABLE（`LOGIN_FAIL_*_EXCEED`）：`tryBlockFirst` 成功才 `reportEvent`；已封禁则只刷新 TTL。

### 2.4 接线

`DefaultAuthenticationFailureHandler` → `LoginFailureEvent` → `LoginFailureAccessListener` → `LoginFailureProtectionService`。  
IP / 设备取自 Auth 入站头（`In-Inner-Client-Real-IP` / `In-Ca-Sig`），BFF Feign 须原样转发，否则会写成 BFF 网卡 IP，与网关 `BlacklistFilter` 对不齐。  
登录成功清零各维度计数。

## 3. DB 种子（migration 011）

- 分组：`login-auth`（登录路径）、`api-business`（`/pms/**`、`/member/**`、`/security/**`）。
- 限流规则：`login-ip`、`pms-ip`、`member-ip`、`security-ip` 等（详见 migration SQL）。
- 回滚：`rollback_011_security_access_protection_seed.sql`。

## 4. 安全事件

L4 扩展 ACCESS 类型（写入 `security_event`，非 `gateway_blacklist_event`）：

```text
LOGIN_FAIL_IP_EXCEED, LOGIN_FAIL_DEVICE_EXCEED,
LOGIN_FAIL_CLIENT_EXCEED, LOGIN_FAIL_ACCOUNT_IP_EXCEED
```

网关违规仍映射为 `RATE_LIMIT_VIOLATION` / `BLACKLIST_BLOCK`（见 [security-event-center](../security-event-center/SPEC.md)）。`RATE_LIMIT_VIOLATION` 优先级仍为 BEST_EFFORT；**触发**为边沿（见 §1.4）。`BLACKLIST_BLOCK` 同为超阈值边沿。

**封禁审计 Platform API**：`GET /platform/security/policy/events` 仅查 **历史** `gateway_blacklist_event`；新事件请查 `security_event`（Platform 读侧后续 change）。

## 5. 与 L2 账号保护交叉

- L4 修复 `RecordLoginUseCaseService`：`attemptWindowMinutes` 滑动窗口重置已上线（见 [account-protection](../account-protection/SPEC.md)）。
- 账号 lockout 仍由 `ingot.security.account.lockout` 配置，remote 中心化**不在** L4 范围。

## 6. 已知限制

1. HTTP Method 不参与 Sentinel 路径匹配。
2. `ingot.security.challenge` SDK 执行面未在 L4 启用。
3. Platform 安全事件分页查询 / 封禁审计新 UI 未交付。
4. 同 IP 试多个账号的行为型防爆破未实现。
5. local 限流与 remote 快照 **独立**：切 `ratelimit.mode=local` 时不读 LKG；共享 Actuator 来源可能仍反映其他 remote 域。
