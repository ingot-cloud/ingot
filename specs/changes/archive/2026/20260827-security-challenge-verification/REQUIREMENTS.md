# Requirements

来源：[安全中心服务需求 §六 6.1](../../../../../docs/requirements/themes/security-center-service.md) 验证码。MFA（6.2）、二次确认（6.3）、挑战记录（6.4）为非目标。

## 用户场景

### S1 登录强制挑战（ALWAYS）

- 使用者：未登录用户提交账号密码。
- 触发条件：挑战域开启；存在匹配 `POST /bff/auth/login` 的 `ALWAYS` 策略；请求未带有效 PassToken Header。
- 期望结果：
  - 网关返回 HTTP **412**，`code=CHALLENGE_REQUIRED`。
  - `data` 含 `vcType`、`checkPath`、`scope`、`scopeParam`、`passTokenParam`（不含 ttl / remaining）。登录种子下分别为 `image`、`/vc/image/check`、`login`、`In-Vc-Scope`、`In-Vc-Pass-Token`。
  - 请求不到 BFF / Auth，不进行密码校验。

### S2 完成滑块后登录成功

- 使用者：S1 中的同一用户。
- 触发条件：按 412 指示拉取拼图、提交滑块校验，并在登录请求携带签发的 PassToken。
- 期望结果：
  - 按 412 `data` 动态验码：`POST {checkPath}`，Header `{scopeParam}={scope}`，成功后 `data[passTokenParam]` 为 PassToken；**不含**嵌套 `captcha`。
  - 重试原登录请求，Header 带 `{passTokenParam}` 与 `{scopeParam}`，进入 BFF 登录编排，不再 412。
  - PassToken 按策略 `passTokenRemaining` 消费；用尽或过期后再次登录回到 S1。

### S3 敏感接口按策略挑战

- 使用者：已登录或匿名客户端访问被策略覆盖的路径（例如密码重置）。
- 触发条件：管理员在安全中心或 Nacos 为该路径配置 `ALWAYS`（或 `ON_RATE_LIMIT`）。
- 期望结果：行为同 S1/S2；不同 `scope` 的 PassToken 互不通用。本闭环不预置敏感接口种子，只保证策略可配置且立即生效。

### S4 限流后挑战（ON_RATE_LIMIT）

- 使用者：打满 Sentinel 限流的客户端。
- 触发条件：路径命中限流，且存在匹配的 `ON_RATE_LIMIT` 策略（该路径 **没有** ALWAYS，或 ALWAYS 未命中）。
- 期望结果：
  - 本次被限流的请求返回 **412** 而非 **429**。无该策略时仍为 429。
  - 验码使用 412 给出的 `scope` 签发 PassToken。
  - **重试必须同时带** Header `In-Vc-Pass-Token` 与 `In-Vc-Scope`（与签发时相同），且当前路径须被该 scope 策略覆盖；消费成功后打 `ATTR_PASS_TOKEN_OK`，**本次跳过 Sentinel**。
  - 策略 `scope` 为自定义值（如 `anon`）时同样生效，不得因代码只认 ALWAYS/`default` 而再次 412/429。
  - 次数用尽或过期后再次限流，行为回到本场景开始。

> **现网 bug（L6 必须修）**：`ChallengeFilter` 消费时只用 ALWAYS 的 `scope`，没有 ALWAYS 则用 `default`。`ON_RATE_LIMIT` 按策略 scope 签发后，重试对不上 Redis key，跳过限流失败。详见 DESIGN D10。

### S5 安全中心改策略热更新（remote）

- 使用者：平台管理员。
- 触发条件：`policy.mode=remote`，Platform 新增 / 修改 / 删除 / 启停挑战策略。
- 期望结果：
  - 写操作发布 `SecurityPolicyDomain.CHALLENGE_POLICY` 失效。
  - 网关在失效后下一次匹配使用新策略（与现有共享快照 evict 一致，目标 ≤10s），无需重启。
  - 前端提示「规则将在数秒内生效」。

### S6 未部署安全中心（local）

- 使用者：只部署 Gateway + BFF + Auth 的环境。
- 触发条件：`ingot.security.challenge.enabled=true`，`policy.mode=local`。
- 期望结果：
  - 策略来自 Nacos `ingot.security.challenge.policy.groups/policies`（`in-security-gateway.yml` 地板 + 本进程可覆盖）。
  - 登录 412 / PassToken 闭环可用。
  - **没有** Platform 策略页；改 Nacos 后无重启即行为变化（见 DESIGN 动态刷新）。

### S7 安全中心短暂不可用（remote 弹性）

- 使用者：生产网关，`mode=remote`。
- 触发条件：Feign 快照失败。
- 期望结果：与 L4 共享快照阶梯一致——有 LKG 用 LKG，无 LKG 用 Nacos 挑战地板；不出现「无策略 → 登录裸奔」fail-open。Actuator `securitypolicy` 可观察来源。

### S8 静态白名单跳过挑战

- 使用者：命中网关静态白名单的客户端。
- 触发条件：`blacklist.enabled=true` 且 WHITE 命中（现 `BlacklistFilter` 语义）。
- 期望结果：不返回 412，不要求 PassToken。

### S9 Redis 不可用

- 使用者：正在完成挑战或携带 PassToken 重试的客户端。
- 触发条件：`PassTokenStore` 使用的 Redis 读写失败或 Bean 不可用。
- 期望结果：
  - 带 Header `In-Vc-Scope` 的验码接口 **不得** 返回「成功但无 token」（否则客户端会误以为可重试）。
  - 业务请求无有效 token 时仍 412（ALWAYS）或走后续限流。
  - 打点 / warn，不静默放行登录。

### S10 不支持的挑战类型

- 使用者：管理员误把策略 `challengeType` 配成 `SMS` / `EMAIL`。
- 触发条件：该策略被匹配。
- 期望结果：编译或匹配阶段跳过并 warn，不把请求导向不存在的 `/vc/sms/check` 挑战闭环。L6 只执行 `IMAGE` / `SLIDER`（均映射 `/vc/image`）。

### S11 不再随登录请求带验证码

- 使用者：现网或旧前端曾依赖 `ingot.vc.verifyUrls` + `_vc_code`。
- 触发条件：L6 上线后 `POST /bff/auth/login` 不带 `_vc_code`、只按 S1/S2 走。
- 期望结果：
  - 三环境 Nacos `ingot.vc.verifyUrls` 为空（或不再包含登录路径）。
  - 网关 `VCWebFilter` 不再拦截登录。
  - BFF `BffLoginDTO` 继续只有 username/password；过时注释中的 `vcCode` 删除。

### S12 关闭挑战域

- 使用者：运维将 `ingot.security.challenge.enabled=false`。
- 触发条件：网关进程加载该配置。
- 期望结果：不装配 `ChallengePolicyService`；登录不再 412；限流命中回 429。滑块引擎路由仍可存在，但不签发 PassToken。

## 业务规则

### 触发与路径（P0）

1. 挑战是否发生 **只** 由 `ingot.security.challenge` 策略决定，不得再用 `ingot.vc.verifyUrls` / `@VCVerify` 作为登录或敏感接口的安全触发。
2. 保护对象是 **网关对外路径**。登录只覆盖 `POST /bff/auth/login`；BFF → Auth 的 Feign（`/oauth2/pre_authorize` 等）不再套验证码。
3. `groupCode` 与内联 `patternList` 二选一，优先分组；须能复用已有 `login-auth` 分组。
4. 策略路径 **禁止** 匹配 `/vc/**`，避免挑战自己的验码接口形成死循环。Platform 写入时拒绝。
5. 多条策略命中同一路径 + trigger 时，按 `priority` 升序取第一条（现 SDK 行为）。

### 执行模型（P0）

6. `ALWAYS`：无有效 PassToken → 412，终止后续 Sentinel。
7. `ON_RATE_LIMIT`：Sentinel 阻断后若匹配策略则 412，否则 429。PassToken **按签发时的 scope 消费成功** 后跳过 Sentinel（替换「限流即 429 失败」，不是 429 之后再叠加一次挑战）。
8. 不支持 `on_failure_threshold`（登录失败保护仍走 L4 / 账号 lockout）。
9. 白名单跳过挑战（规则 6 的例外）。临时封禁 / 静态黑名单仍在挑战之前 403。
10. 同一路径同时有 ALWAYS 与限流时：无 token 在 Sentinel 之前 412，限流规则对该次请求不执行；有有效 PassToken 时 **连 Sentinel 一并跳过**（与现 `WhitelistAwareSentinelGatewayFilter` 语义一致）。违规升级只统计真正进入 `SentinelBlockHandler` 的阻断（含变成 412 的限流命中）。

### PassToken（P0）

11. Redis key：`in:gw:vc:pass:{scope}:{token}`；value 为剩余次数；TTL = `passTokenTtlSec`。
12. 不同 `scope` 互不通用；策略必须配置非空 `scope`。登录种子 `scope=login`。
13. 请求头名固定：`In-Vc-Pass-Token`、`In-Vc-Scope`（`VCConstants.HEADER_*`，412 的 `passTokenParam` / `scopeParam`）。**消费时 scope 取请求 Header**，与签发一致（D10 / D12）；禁止用「仅 ALWAYS 策略 scope」或硬编码 `default` 去消费限流挑战 token。只放 query 视为无 token。消费前须 `matchByScope` 命中当前路径（D11）。本轮不绑客户端 IP（D13）。
14. `passTokenTtlSec` ≥ 1、`passTokenRemaining` ≥ 1；下限与现 `GatewaySecurityConstants.MIN_PASS_TOKEN_REMAINING` 对齐。
15. Redis 不可用：验码带 scope 时失败；消费失败视为无效 token（D8）。ALWAYS 消费失败仍 412；纯 `ON_RATE_LIMIT` 路径消费失败则进入 Sentinel（可能再次 412/429），不得假装已跳过限流。

### 验证码引擎（P0）

16. L6 执行面只跑图形 / 滑块（anji `blockPuzzle`）。`SLIDER` 与 `IMAGE` 均路由 `/vc/image`。
17. 拉码 `GET /vc/image`，验码 `POST /vc/image/check`（anji `pointJson` + `token`）。验码成功且带合法 Header `In-Vc-Scope` 即签发 PassToken；成功 `data` 不含 `captcha`（D14）。**不再**要求业务请求带 `_vc_code` / `captchaVerification`。
18. 引擎参数（底图、误差、anji 频率、`opsLimit*`）只在 Nacos `ingot.vc.image.*`，不上安全中心。
19. 短信 / 邮箱模块本 change 不删除，但不接入挑战。

### 配置与降级（P0）

20. `enabled` / `policy.mode` 在 `in-service-gateway.yml`（L2）；`policy.groups` / `policies` 在 `in-security-gateway.yml`（L1 地板），地板 **不写** `enabled` / `mode`。
21. `mode=local`：只读 yaml，不读 LKG。
22. `mode=remote`：共享快照 `challengePolicies`；yaml 仅作 Nacos 地板。弹性阶梯与 L4 相同：`remote → LKG → 地板`。
23. local 改 Nacos 地板或本进程 yaml 后，无重启即重新编译策略（`LocalPolicyEnvironmentRefreshListener` 前缀 `ingot.security.challenge.`）。验证方式：改 `enabled` 或登录策略 → 再请求登录行为变化。
24. 生产默认 `enabled=true`、`mode=remote`；DEV 默认 `mode=local` 以便无中心联调。

### 管理面（P0）

25. Platform 权限沿用 `platform:security:policy:query|create|update|delete`。
26. 写入必须校验：`code` 唯一、`trigger` ∈ {`always`,`on_rate_limit`}、`challengeType` ∈ {`SLIDER`,`IMAGE`}（L6）、`scope` 非空、PassToken 数值、路径或分组、禁止 `/vc/**`。
27. 变更后广播 `CHALLENGE_POLICY` 域失效。

### 优先级

- P0：412 闭环、登录种子、remote/local、captcha 与 OTP 拆分、废弃 verifyUrls、Platform 校验、S9 fail-closed、**D10 PassToken scope 消费修复**、D11 路径绑定、D12 Header、D14 验码去 captcha、E2E。
- P1：`ON_RATE_LIMIT` 地板示例（不强制种子）、Actuator 挑战域来源展示已有则保持。
- P2：挑战记录 / 验码失败拉黑 / SMS·EMAIL 挑战。

## 边界与非目标

- **前端仓库**：本 change 只提供 [PLATFORM-API.md](./PLATFORM-API.md) 契约；HTTP 客户端须 **全局拦截** 412，按 `data` 动态拉码/验码后重试原请求，不要只改登录页。
- **破坏性**：上线后旧「登录 body/query 带验证码」路径失效；须与前端同发。可用 `enabled=false` 回滚执行面。
- **兼容**：不改变 L4 限流 / 名单 / 违规升级 / 登录失败保护语义；不改变 L5 会话模型。
- **表结构**：不删 `failure_*` / `challenge_failure_limit` / `block_ttl_sec` 列；执行面继续不读。
- **事件**：不新增 `CHALLENGE_*` 事件类型。
- **BFF 内部**：不在 `BffAuthService.login` 内调验证码服务。

## 验收标准

- [x] A1：无 PassToken 访问 `POST /bff/auth/login` → 412，`data` 含 `vcType`/`checkPath`/`scope`/`scopeParam`/`passTokenParam`，**不含** `ttlSec`/`remaining`。
- [x] A2：滑块 check 带 Header `In-Vc-Scope: login` 返回 `data["In-Vc-Pass-Token"]`（无 `captcha`）；持该 token **且** Header `In-Vc-Scope: login` 重试登录进入 BFF，网关不再 412。
- [x] A3：错误 / 过期 / 错误 scope 的 token 不能放行 ALWAYS 登录。
- [x] A4：`mode=remote` 时 Platform 新增 ALWAYS 策略（非登录路径）→ 无重启，该路径开始 412；删除或停用后恢复。
- [x] A5：`mode=local` 时不部署安全中心，仅靠 Nacos 地板完成 A1/A2；改地板策略 `enabled=false` 无重启后登录不再 412。
- [x] A6：`mode=remote` 停安全中心 → 登录挑战仍按 LKG 或地板执行，不 fail-open。
- [x] A7：三环境 `ingot.vc.verifyUrls` 为空；登录不要求 `_vc_code`。
- [x] A8：`challenge.enabled=false` 时登录不 412。
- [x] A9：白名单 IP 访问登录不 412（须 `blacklist.enabled=true` 且存在 WHITE）。
- [x] A10：Redis 不可用时，带 Header `In-Vc-Scope` 的 check **不**返回成功 token；登录仍 412。
- [x] A11：策略 `challengeType=SMS` 不产生可用的短信挑战闭环（跳过或 412 不指向 `/vc/sms`）。
- [x] A12：限流命中 + `ON_RATE_LIMIT` 策略 → 412；无该策略 → 429（E2E TC-SP-013/014）。
- [x] A13：PassToken 签发逻辑不再被注释；`CaptchaVCProcessor` 不再按 `/auth/oauth2/token` 做 grant 特例。
- [x] A14：D1–D14 已在 DESIGN 闭合，实现与决议一致。
- [x] A15：[PLATFORM-API.md](./PLATFORM-API.md) 与 Controller / 412 响应体一致。
- [x] A16：Nacos 动态刷新验证记录在 TASKS：改配置 → 无重启 → 行为变化。
- [x] A17：仅 `ON_RATE_LIMIT`、策略 `scope` 非 `default`（如 `e2e-anon`）时：412 → check 签发 → 重试同时带 Header token 与 scope → **200 且跳过 Sentinel**（不再二次 412/429）。缺 Header scope、scope 错误、或路径未覆盖该 scope 则不能跳过限流。 login 的 token 打非 login 路径不得 consume。
