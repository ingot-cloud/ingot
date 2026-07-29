# Requirements

## 用户场景

### S1 登录路径 SDK 限流生效（Closure）

- 使用者：任意匿名或已登录客户端访问登录相关路径。
- 触发条件：网关 `ingot.security.ratelimit.enabled=true`、`policy.mode=remote`，安全中心存在 `login-auth` 分组及 IP 维度限流规则。
- 期望结果：
  - 同一 IP 在配置窗口内超 QPS → HTTP **429**，`code=LIMIT_TOO_MANY`。
  - 反复触发限流且 `violation-escalation` 达阈值 → 后续请求 **403**，`code=FORBIDDEN_BLOCKED`（临时封禁）。
  - 规则变更经 Platform CRUD + 失效广播后 **≤10s** 生效，无需重启网关。

### S2 旧版 RequestRateLimiter 迁移移除

- 使用者：访问 `/pms/**`、`/member/**`、`/security/**` 的客户端。
- 触发条件：SDK 已为上述前缀配置等价或更细 Sentinel 规则，E2E 迁移用例通过。
- 期望结果：
  - Nacos 路由定义中 **不再** 包含 `RequestRateLimiter` filter。
  - 限流行为由 SDK + Sentinel 统一承担，可通过 Platform 动态调整。

### S3 IP 维度登录失败达阈值临时封禁

- 使用者：同一 IP 反复登录失败（password grant / BFF 登录）。
- 触发条件：`ingot.security.access.login-failure.ip.enabled=true`，窗口内失败次数 ≥ `maxAttempts`。
- 期望结果：
  - Redis 写入与网关 `TempBlockStore` **共用 key 前缀** 的临时封禁。
  - 后续该 IP 请求在 `BlacklistFilter` 返回 **403**。
  - `ingot_security.security_event` 出现 ACCESS 类事件（`LOGIN_FAIL_IP_EXCEED` 或 DESIGN 锁定类型）。
  - **不**触发账号 lockout（与 L2 独立）。

### S4 设备维度登录失败达阈值临时封禁

- 使用者：携带 `In-Ca-Sig` 设备指纹的客户端。
- 触发条件：设备维度开关开启，窗口内失败次数达阈值。
- 期望结果：按设备指纹临时封禁，行为同 S3；BFF 登录路径须透传 `In-Ca-Sig`。

### S5 Client 维度登录失败达阈值临时封禁

- 使用者：使用固定 OAuth2 `client_id` 的应用。
- 触发条件：Client 维度开关开启，窗口内失败次数达阈值（含 `invalid_grant` 密码错误，不含纯 `invalid_client` 除非 DESIGN 决策扩展）。
- 期望结果：按 `client_id` 临时封禁；网关侧 `RateLimitDimension.CLIENT` / 名单 `CL` 维度可对齐。

### S6 账号+IP 组合维度登录失败达阈值临时封禁

- 使用者：针对同一账号从同一 IP 反复失败（撞库 / 定向爆破）。
- 触发条件：组合维度开关开启，复合键 `{userType}:{username}:{ip}` 窗口内失败达阈值。
- 期望结果：临时封禁该 IP（D3 默认：不叠加账号 lockout，避免双惩罚）；组合键成功登录后清零。

### S7 登录成功清零失败计数

- 使用者：各维度保护对象。
- 触发条件：登录成功事件发布。
- 期望结果：对应 IP / 设备 / Client / 账号+IP 的 Redis 失败计数清零；账号维度 `failed_login_count` 仍走 L2 逻辑。

### S8 账号 lockout 滑动窗口（`attemptWindowMinutes`）

- 使用者：任意 ADMIN / Member 用户。
- 触发条件：`attemptWindowMinutes=15`，首次失败后超过 15 分钟无新失败。
- 期望结果：`account_lock_state.failed_login_count` 在窗口外自动归零（或视为过期不计入阈值），不必等登录成功或解锁。

### S9 网关策略 remote 弹性降级

- 使用者：网关实例，`policy.mode=remote`，安全中心短暂不可用。
- 触发条件：Feign 快照连续失败。
- 期望结果：
  - 有 LKG → 使用最近成功快照编译 Sentinel/名单规则，Actuator 显示 `LAST_KNOWN_GOOD`。
  - 无 LKG → 使用 Nacos 地板（`in-security-policy.yml` 内各域 baseline），Actuator 显示 `LOCAL_FLOOR`。
  - **不出现**「RemoteSnapshotFetcher 返回 null → 无限流无名单」fail-open。
  - 远程恢复后下一次 fetch 成功即刷新 LKG，来源回到 `REMOTE`。

### S10 未部署安全中心或 local 模式

- 使用者：仅网关 + Auth + PMS/Member，未部署 `ingot-security`。
- 触发条件：`ingot.security.access.mode=local`（或 gateway `ingot.security.*.policy.mode=local`），或 remote 不可用且无 LKG/地板。
- 期望结果：
  - 登录、锁定（L2）主链路正常。
  - 登录失败保护读 Nacos `ingot.security.access.login-failure.*` **地板**配置。
  - 网关限流读 `RateLimitProperties` 等 local yaml；地板禁用且无 LKG 时 fail-closed（D7）。

### S11 配置热刷新

- 使用者：运维 / 安全管理员。
- 触发条件：运行中修改 Nacos 地板 yaml，或 Platform 修改登录失败/网关策略，不重启。
- 期望结果：下一次请求/登录失败判定按新配置执行；Platform 写操作触发对应域失效广播。

### S12 安全中心 Platform 管理登录失败保护策略（remote）

- 使用者：安全管理员（Platform 前端）。
- 触发条件：调用 `GET/PUT /platform/security/access/login-failure-policies`；Auth `ingot.security.access.mode=remote`。
- 期望结果：
  - 四维策略（IP/DEVICE/CLIENT/ACCOUNT_IP）可在管理台 CRUD，持久化至 `login_failure_protection_policy`。
  - 保存后 `SecurityPolicyDomain.LOGIN_FAILURE_PROTECTION` 失效广播，Auth **≤10s** 内按新阈值判定。
  - remote 不可用时 Auth 走 LKG → Nacos 地板，**不** fail-open 为「无保护」。
  - 前端对接字段与交互见 [PLATFORM-API.md](./PLATFORM-API.md)。

## 业务规则

1. **Sentinel 为唯一网关限流执行引擎**：旧 `RequestRateLimiter` 迁移完成后不得并存。（P0）
2. **白名单式限流**：未配置路径默认不限流；全站保护通过 broad endpoint group 显式配置。（P0）
3. **四维度独立开关**：IP / 设备 / Client / 账号+IP 可分别 `enabled` / 阈值 / 窗口 / 封禁 TTL。（P0）
4. **账号 lockout 与访问防护解耦**：账号维度锁定仍仅由 L2 `RecordLoginUseCaseService` 负责；L4 不因 IP 封禁自动锁定账号，反之亦然（除非将来风险规则 change 显式联动）。（P0）
5. **临时封禁 key 与网关一致**：Auth 侧写入的封禁须被 `BlacklistFilter` / `TempBlockStore` 识别（共用 Redis key 前缀 `in:gw:bl:tmp:`）。（P0）
6. **事件上报不阻塞主链路**：ACCESS 事件经 L3 异步 reporter；失败仅 warn。（P0）
7. **remote 合法空快照 = 合法无规则**：成功返回空列表时刷新 LKG，不触发兜底。（P1，对齐 credential D-B）
8. **Nacos 地板非空**：local 模式地板 yaml 须含最低安全基线（至少登录路径 IP 限流 + violation-escalation 默认）。（P0）
9. **BFF 设备指纹约束**：C 端登录须透传 `In-Ca-Sig`；缺失时设备维度跳过计数（不 fail）。（P1）

### P0 ACCESS 事件类型（本期新增）

| event_type | 说明 |
|------------|------|
| `LOGIN_FAIL_IP_EXCEED` | IP 维度登录失败达阈值并临时封禁 |
| `LOGIN_FAIL_DEVICE_EXCEED` | 设备维度登录失败达阈值并临时封禁 |
| `LOGIN_FAIL_CLIENT_EXCEED` | Client 维度登录失败达阈值并临时封禁 |
| `LOGIN_FAIL_ACCOUNT_IP_EXCEED` | 账号+IP 组合维度达阈值并临时封禁 |

（命名可在 DESIGN 审阅时微调，须入 `SecurityEventType` enum。）

### 优先级

- P0：Resilience 阶梯、SDK 启用、种子规则、RequestRateLimiter 移除、四维度保护、**登录失败 remote + Platform CRUD + Auth Resilient**、attemptWindowMinutes、S9–S12、[PLATFORM-API.md](./PLATFORM-API.md) 与实现一致。
- P1：Client 维度网关 `RateLimitDimension` / 名单扩展、Actuator 指标（login-failure 来源可观测）。
- P2：单元测试 ResilientSnapshotFetcher、ResilientLoginFailurePolicyLoader、LoginFailureProtectionService、滑动窗口边界。

### 与其他模块关系

- **L2 账号保护**：互补；L4 不实现 **账号 lockout** remote 中心化（仍为 L2 后续 change）。
- **L3 安全事件中心**：复用 `RemoteSecurityEventService`；安全事件 Platform 查询仍不在 L4。
- **前端管理台**：网关策略 + 登录失败保护 API 见 [PLATFORM-API.md](./PLATFORM-API.md)。
- **L6 挑战验证**：不在本 change 启用 `ingot.security.challenge` SDK。
- **ingot.vc**：图形验证码 ops 限速保持独立；不与 challenge SDK 合并。

## 边界与非目标

- **异常场景**：Redis 不可用 → 登录失败计数降级为 no-op（仅 warn，不阻断登录）；临时封禁 no-op 时网关不 403（与现有 `TempBlockStore` 行为一致）。
- **兼容**：
  - 不改变 L2 账号 lockout 判定语义（除 `attemptWindowMinutes` 补齐）。
  - L3 事件上报语义不变；仅扩展 event_type。
  - `gateway_blacklist_event` 仍只读，不写新数据。
- **非目标**：见 [README](./README.md)「不包含」。
- **边界**：`invalid_client` 与 password grant 失败分场景计数（DESIGN D10）。
- **边界**：自动封禁在「本次失败异步处理完成后」生效，当次认证响应仍返回 bad credentials。

## 验收标准

- [ ] S1：remote 配置下登录路径限流 429 → 违规升级 403；Platform 改规则热更新生效
- [ ] S2：`/pms/**`、`/member/**`、`/security/**` 路由无 `RequestRateLimiter`；SDK 规则覆盖且 E2E 通过
- [ ] S3–S6：四维度独立开关验证；达阈值 403 + 中心库 ACCESS 事件
- [ ] S7：登录成功后各维度 Redis 计数清零
- [ ] S8：`attemptWindowMinutes` 窗口外失败计数归零，账号 lockout 阈值行为正确
- [ ] S9：security 宕机 → 网关 LKG → 地板；Auth login-failure remote 宕机 → Auth LKG → 地板；Actuator 来源正确，无 fail-open
- [ ] S10：local 模式 / 无 security 时主链路正常
- [ ] S11：Platform 或 Nacos 改配置不重启，行为符合新配置
- [ ] S12：Platform CRUD 登录失败四维策略；Auth remote 拉取生效；实现与 [PLATFORM-API.md](./PLATFORM-API.md) 一致
- [ ] L2 / L3 回归：账号锁定与 AUTH 事件无破坏
- [ ] migration `011` 可执行且可回滚；相关模块编译通过
