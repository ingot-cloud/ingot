# 挑战验证 SPEC

> 记录当前已验收并在线生效的系统事实（L6 As-Built）。

## 1. 执行模型

Filter 顺序见 [access-protection](../access-protection/SPEC.md)：`BlacklistFilter` → `ChallengeFilter` → Sentinel。

| 触发 | 位置 | 无有效 PassToken |
|---|---|---|
| `ALWAYS` | `ChallengeFilter` | HTTP **412** `CHALLENGE_REQUIRED`，不到 Sentinel |
| `ON_RATE_LIMIT` | `SentinelBlockHandler` | 限流命中后 **412**；无匹配策略则 **429** |

白名单（`ATTR_WHITELISTED`）跳过挑战与 Sentinel。`/vc/**` 不做 ALWAYS。

有效 PassToken 消费成功后打 `ingot.security.passToken.ok`，本次 **跳过 Sentinel**。

登录保护对象是网关 **`POST /bff/auth/login`**。BFF 登录 JSON 不含验证码字段；BFF → Auth Feign 不再套码。

## 2. PassToken

| 项 | 事实 |
|---|---|
| Redis key | `in:gw:vc:pass:{scope}:{token}` |
| value | 剩余可消费次数（整数） |
| TTL | 策略 `passTokenTtlSec`（≥ 1） |
| 签发 | `POST /vc/image/check` 滑块成功且 Header `In-Vc-Scope` 能 `findByScope` |
| 消费 scope | 只认请求头 `In-Vc-Scope`，禁止回退 ALWAYS scope 或 `default` |
| 路径绑定 | 消费前 `matchByScope(path, method, scope)`；不命中则不 DECR、不跳过限流 |
| Redis 不可用 | 带 scope 的 check fail-closed；消费视为无效 |

传输：**请求头**（禁止 query、禁止 `In-Inner-*`）：

| Header | 常量 | 用途 |
|---|---|---|
| `In-Vc-Scope` | `VCConstants.HEADER_SCOPE` | 与 412 `data.scope` 相同 |
| `In-Vc-Pass-Token` | `VCConstants.HEADER_PASS_TOKEN` | 通行证 |

412 `scopeParam` / `passTokenParam` 即上述头名。验码成功 `data` 只含这两个键，**不含**嵌套 `captcha`。无 Header scope 的 check 仍返回 anji `ResponseModel`。

## 3. 策略与配置

各域独立：`ingot.security.challenge.enabled` + `policy.mode`。

| 环境 | enabled | mode |
|---|---|---|
| 代码缺省 | `false` | `local` |
| DEV Nacos | `true` | `local` |
| TEST / PROD Nacos | `true` | `remote` |

- L2 开关 / mode：`in-service-gateway.yml`
- L1 地板 `groups` / `policies`：`in-security-gateway.yml`（**不写** enabled/mode）
- 引擎：`ingot.vc.image.*` 仅 Nacos，不上安全中心
- `ingot.vc.verifyUrls` 三环境为空，不再作为登录安全触发

`local`：只读 yaml，`LocalPolicyEnvironmentRefreshListener` 前缀 `ingot.security.challenge.` → `evictAll()`。  
`remote`：共享快照 `challengePolicies`；弹性 `remote → LKG → 地板`。

编译：仅 `IMAGE` / `SLIDER`（均映射 `/vc/image`）；`SMS` / `EMAIL` 跳过。`groupCode` 优先于内联 `patternList`；分组来自**挑战域** groups（不是限流 yaml 的 groups）。路径 method 通配字面量必须是 **`ANY`**，不认 `*`。

### 3.1 种子

migration `020`：`security_challenge_policy.code=login-always`，`group_code=login-auth`，`trigger=always`，`challenge_type=SLIDER`，`scope=login`，`pass_token_ttl_sec=300`，`pass_token_remaining=3`。

Nacos 地板：`login-always-floor` 覆盖同一登录路径。

表列 `failure_*` / `challenge_failure_limit` / `block_ttl_sec` **执行面不读**。

## 4. HTTP 契约（摘要）

412 `data`：`vcType`、`checkPath`、`scope`、`scopeParam`、`passTokenParam`。不返回 `ttlSec` / `remaining`。

客户端全局拦截 412 → `GET /vc/{vcType}` 拉码 → `POST {checkPath}` 带 `In-Vc-Scope` → 原请求带两个 Header 重试。完整字段见归档 [PLATFORM-API.md](../../../changes/archive/2026/20260827-security-challenge-verification/PLATFORM-API.md)。

Platform：`/platform/security/policy/challenges`；写入校验 scope、类型、PassToken 数值、路径或分组、禁止 `/vc/**`；变更发 `CHALLENGE_POLICY` 失效。

## 5. 已知限制

1. PassToken **不绑客户端 IP**。
2. 验码失败不按策略拉黑。
3. 不写 `CHALLENGE_*` 安全事件。
4. 前端仓库不在本能力范围；未接 412 拦截的旧登录会失败。
5. pattern `method` 必须是 `ANY` 或具体动词；前端传 `*` 会导致挑战匹配失败（限流仍可能 429）。
