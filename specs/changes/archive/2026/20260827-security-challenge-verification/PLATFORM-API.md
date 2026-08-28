# 挑战验证 Platform API 与客户端契约

> **受众**：安全中心管理台前端、登录 / 业务前端  
> **服务**：管理面 `ingot-service-security`（网关 `/security/**` StripPrefix）；执行面 `ingot-gateway`  
> **响应包装**：统一 `R<T>`（`code` / `message` / `data`）  
> **Change**：`20260827-security-challenge-verification`（L6）

L4 文档曾将挑战策略标为「可只读、前端可隐藏」。L6 **启用执行面**，管理台必须可配，登录页必须处理 412。

网关限流 / 名单等其它策略仍见归档 [L4 PLATFORM-API](../../archive/2026/20260729-security-access-protection/PLATFORM-API.md)。

---

## 1. 通用约定

### 1.1 管理面请求头

| Header | 必填 | 说明 |
|---|---|---|
| `Authorization` | 是 | `Bearer {access_token}` |
| `Content-Type` | 写操作 | `application/json` |

### 1.2 响应结构

```json
{
  "code": "S0200",
  "message": "Success",
  "data": { }
}
```

失败时 `code` 非 `S0200`。网关挑战失败用 HTTP 状态码 + 业务 `code`（见 §3），不要与 `S0200` 混淆。

### 1.3 权限码

挑战策略与其它网关策略共用：

| 操作 | 权限 |
|---|---|
| 查询 | `platform:security:policy:query` |
| 新增 | `platform:security:policy:create` |
| 更新 | `platform:security:policy:update` |
| 删除 | `platform:security:policy:delete` |

### 1.4 热更新

写操作成功后后端发布 `SecurityPolicyInvalidationEvent`，域 = `CHALLENGE_POLICY`。网关订阅后清共享快照 L1/L2（不清 LKG）。

前端提示：**「规则将在数秒内生效」**。可选「强制刷新」仍调用既有 `POST /platform/security/policy/broadcast-invalidation`（全域）。

### 1.5 页面建议

```text
安全中心
└── 访问防护
    ├── API 路径分组
    ├── 限流规则
    ├── 黑白名单
    ├── 违规升级
    ├── 登录失败保护
    └── 挑战策略          ← L6 可写
```

**所有经网关的前端请求**（登录、业务 API、表单提交）都必须按 §3 **统一拦截 HTTP 412**，不要只在登录页处理。不能再依赖「业务请求随带验证码」。

---

## 2. 挑战策略 CRUD

Base：`/platform/security/policy`

网关对外路径经 `/security` StripPrefix 后即上表路径。

### 2.1 GET `/challenges`

查询全部挑战策略，按 `priority` 升序。

**权限**：`platform:security:policy:query`

**响应 `data`**：数组。字段与写入体相同，另含 `id`、`createdAt`、`updatedAt`。

### 2.2 POST `/challenges` · PUT `/challenges`

新增 / 更新。PUT **必须带 `id`**。

**权限**：create / update

**Body 示例**（登录 ALWAYS，复用已有分组 `login-auth`）：

```json
{
  "code": "login-always",
  "groupCode": "login-auth",
  "trigger": "always",
  "challengeType": "SLIDER",
  "scope": "login",
  "passTokenTtlSec": 300,
  "passTokenRemaining": 3,
  "enabled": true,
  "priority": 0,
  "remark": "登录前滑块挑战"
}
```

内联路径（不引用分组）时 `groupCode` 置空，填 `patternList`：

```json
{
  "code": "reset-password-always",
  "patternList": [
    { "path": "/bff/auth/password/reset", "method": "POST" }
  ],
  "trigger": "always",
  "challengeType": "SLIDER",
  "scope": "reset-password",
  "passTokenTtlSec": 120,
  "passTokenRemaining": 1,
  "enabled": true,
  "priority": 10
}
```

> 上例路径仅作产品示意；仓库内是否存在该 API 以实际路由为准。敏感接口 **不写死**，由本页配置。

### 2.3 DELETE `/challenges/{id}`

**权限**：`platform:security:policy:delete`

### 2.4 字段

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `id` | long | PUT 是 | 主键 |
| `code` | string | 是 | 全局唯一，建议英文+数字+连字符 |
| `groupCode` | string | 与 `patternList` 二选一 | 引用 `GET /groups` 的 `code`。非空时忽略 `patternList` |
| `patternList` | array | 与 `groupCode` 二选一 | `[{ "path": "/bff/auth/login", "method": "POST" }]`；`path` 为网关对外 Ant 路径 |
| `trigger` | string | 是 | `always`：匹配即 412；`on_rate_limit`：限流命中后 412。不要传 `on_failure_threshold` |
| `challengeType` | string | 是 | L6 仅 `SLIDER` 或 `IMAGE`（均走 `/vc/image`）。不要配 `SMS` / `EMAIL` |
| `scope` | string | 是 | PassToken 命名空间，不同策略不要复用除非有意共享令牌 |
| `passTokenTtlSec` | int | 是 | ≥ 1，建议登录 300 |
| `passTokenRemaining` | int | 是 | ≥ 1，建议登录 3 |
| `enabled` | boolean | 是 | false 则不参与匹配 |
| `priority` | int | 否 | 越小越先匹配，默认 0 |
| `remark` | string | 否 | |
| `failureDimension` / `failureThreshold` / `failureWindowSec` | — | 否 | **废弃**，不要在表单展示 |
| `challengeFailureLimit` / `blockTtlSec` | int | 否 | 管理面保留；**网关不执行**验码失败拉黑 |

### 2.5 前端表单校验（与后端一致）

- `code` 必填。
- `groupCode` 与 `patternList` 至少一项。
- `trigger` 只能是 `always` 或 `on_rate_limit`（大小写提交前建议规范为小写或与后端约定一致）。
- `challengeType` 只能是 `SLIDER` 或 `IMAGE`。
- `scope` 非空，最长 64。
- `passTokenTtlSec` ≥ 1，`passTokenRemaining` ≥ 1。
- `path` 不得匹配验证码接口：禁止 `/vc`、`/vc/**`。
- 登录建议：分组选 `login-auth`（路径 `POST /bff/auth/login`），`scope=login`，与种子一致，避免重复 ALWAYS。

### 2.6 种子与地板

| 来源 | 内容 |
|---|---|
| DB（remote） | `code=login-always`，`groupCode=login-auth`，`trigger=always`，`challengeType=SLIDER`，`scope=login` |
| Nacos 地板（local / remote 末级） | 等价策略 `login-always-floor`，路径同样是 `POST /bff/auth/login` |

管理台列表在 remote 生产应能看到种子行，可停用（`enabled=false`）以关闭登录挑战。

---

## 3. 客户端：统一处理 412（任意请求）

执行者不在安全中心服务，而在 **网关**。浏览器只与网关公网入口交互。

**前端必须在 HTTP 客户端（axios / fetch 封装）做全局拦截**，不要只写在登录页。登录、限流后的业务接口、日后新增的 ALWAYS 路径，返回形态相同。

### 3.1 拦截判定

同时满足才走挑战流程，否则按普通错误处理：

1. HTTP 状态码 **412**
2. 响应体 `code === "CHALLENGE_REQUIRED"`
3. `data` 含下方动态字段（缺字段则不要猜，当作失败）

| HTTP | `code` | 前端 |
|---|---|---|
| **412** | `CHALLENGE_REQUIRED` | **暂停原请求** → 弹滑块 → 按 `data` 验码 → **原样重试**该请求 |
| 403 | `FORBIDDEN_BLOCKED` | 封禁提示，不是验证码 |
| 403 | `ACCOUNT_LOCKED` | 账号锁定 |
| 429 | `LIMIT_TOO_MANY` | 限流；本路径没有 `ON_RATE_LIMIT` 挑战时才会出现 |
| 其它 | — | 业务错误，与挑战无关 |

不要把 412 当成「登录失败 / 参数错误」直接 toast 后丢弃。

### 3.2 何时会出现 412

| 条件 | 结果 |
|---|---|
| `ingot.security.challenge.enabled=false` | 不会 412 |
| 静态白名单命中 | 不会 412 |
| 黑名单 / 临时封禁 | **403** `FORBIDDEN_BLOCKED`，先于挑战 |
| 匹配 `ALWAYS` 且无有效 PassToken | **412** |
| Sentinel 限流且匹配 `ON_RATE_LIMIT` | **412** |
| Sentinel 限流且无挑战策略 | **429** `LIMIT_TOO_MANY` |

登录种子启用后，`POST /bff/auth/login` 走 ALWAYS。其它路径由策略决定，前端 **不维护路径白名单**。

### 3.3 412 `data` 字段（动态契约）

HTTP **412 Precondition Failed**。字段名与取值都以后端本次响应为准，**禁止写死** `/vc/image`、`In-Vc-Scope`、`In-Vc-Pass-Token`。

```json
{
  "code": "CHALLENGE_REQUIRED",
  "msg": "Captcha required",
  "data": {
    "vcType": "image",
    "checkPath": "/vc/image/check",
    "scope": "login",
    "scopeParam": "In-Vc-Scope",
    "passTokenParam": "In-Vc-Pass-Token"
  }
}
```

| 字段 | 类型 | 前端必须怎么用 |
|---|---|---|
| `vcType` | string | 拉码：`GET /vc/{vcType}`。L6 为 `image`（滑块也走 image，**没有** `/vc/slider`） |
| `checkPath` | string | 验码：`POST {checkPath}` |
| `scope` | string | 作用域 **值**，原样作为 Header 值；不同策略不同（登录常为 `login`，限流可能是 `anon`） |
| `scopeParam` | string | 作用域 **请求头名**。验码与重试都要带该头，值为 `scope` |
| `passTokenParam` | string | PassToken **请求头名**。验码成功后从 `check` 的 `data[passTokenParam]` 取值；重试业务请求带该头 |

**不返回** `ttlSec`、`remaining`。有效期与可消费次数只在服务端 Redis 执行；过期或用尽会再次 412，拦截器再走一遍即可。

Header：`WWW-Authenticate: Captcha realm="{vcType}"`（可忽略，以 body 为准）。

跨域：若网关 CORS 不是 `allowedHeaders: *`，须显式允许 `In-Vc-Scope`、`In-Vc-Pass-Token`。不要使用 `In-Inner-*` 头名（网关会剥离）。

### 3.4 全局处理流程

对「被 412 打断的那一次请求」记为 `original`（方法、URL、headers、body 全部保留）。

```text
任意 API
  → 412 + CHALLENGE_REQUIRED
  → 弹出滑块（同一时刻只处理一个挑战，其余请求排队或等本次完成）
  → GET /vc/{data.vcType}                         拉码
  → 用户完成滑块
  → POST {data.checkPath}
       Header {data.scopeParam}: {data.scope}
       另附 anji 要求的 token、pointJson
  → 从验码响应 data 取 token = data[data.passTokenParam]
  → 重试 original：原 method / path / body 不变
       Header 增加 {data.passTokenParam}: {token}
                 {data.scopeParam}: {data.scope}
  → 成功则交给原调用方；仍 412 则重新挑战（建议设重试上限，如 2 次）
```

伪代码：

```text
if (status == 412 && body.code == "CHALLENGE_REQUIRED") {
  d = body.data
  captcha = GET  "/vc/" + d.vcType
  // 用户完成滑块，得到 anji token + pointJson
  checked = POST d.checkPath  with Header d.scopeParam = d.scope
  token = checked.data[d.passTokenParam]
  retry original with Headers:
      d.passTokenParam = token
      d.scopeParam     = d.scope
}
```

约束：

- PassToken **只放 Header**，不要放进 JSON body，也 **不要写 query**（例如 `BffLoginDTO` 没有验证码字段）。
- `{scopeParam}` 的值必须与本次 412 的 `data.scope`、验码时使用的值 **完全一致**。只带 token、不带 scope 时，限流挑战 **无法跳过 Sentinel**。
- 该 token 只能用于 412 对应策略覆盖的路径；login 的 token 不能拿去打其它接口并跳过限流。
- 验码失败（滑块错、签发失败）**不要**重试 `original`，让用户再滑一次。
- 拉码 / 验码请求本身不要再套一层 412 拦截（`/vc/**` 网关不做 ALWAYS）。
- 并发：多个请求同时 412 时，建议只弹一次滑块，拿到 token 后按各自的 `scope` 重试；**不要**把 `login` 的 token 用到 `anon` 请求上。

### 3.5 拉码 `GET /vc/{vcType}`

- 无需登录。`vcType` 来自 412，不要写死。
- 响应为 anji `ResponseModel`（拼图 token、底图等），与 anji-plus 滑块 SDK 对齐。
- 频率受 `ingot.vc.image.opsLimitGetPerMinute` 限制，超限是验证码模块错误，不是 412。

### 3.6 验码 `POST {checkPath}`

| 参数 | 位置 | 说明 |
|---|---|---|
| `{scopeParam}`（如 `In-Vc-Scope`） | **Header** | **必须**，值 = 412 的 `data.scope` |
| `token` | query 或表单 | anji 拉码返回的 token |
| `pointJson` | query 或表单 | 滑块轨迹 / 坐标（anji 约定） |

**成功（已带合法 scope Header）**：

```json
{
  "code": "S0200",
  "data": {
    "In-Vc-Pass-Token": "无连字符 hex/uuid",
    "In-Vc-Scope": "login"
  }
}
```

token / scope 的 JSON 字段名等于 412 的 `passTokenParam` / `scopeParam`，用 `data[passTokenParam]` 读取，不要写死头名。**不要**依赖 `data.captcha`（签发成功体不含 anji `ResponseModel`）。不要再用 `captchaVerification` 跟业务请求走。

**失败**：滑块错误、scope 无对应策略、Redis 无法签发。不要重试业务请求。

未带 `{scopeParam}` Header：可能只返回 anji 结果且 **没有** PassToken，原请求仍会 412。挑战场景必须带 scope。只把 scope 放 query **不会**签发。

### 3.7 与旧模型的差异（破坏性）

| 旧（`verifyUrls`） | 新（L6） |
|---|---|
| 业务请求带 `_vc_code` / captchaVerification | 先 412，再带 PassToken **Header** 重试 |
| 只处理登录页 | **所有**经网关请求统一拦截 412 |
| 配置在 `ingot.vc.verifyUrls` | 配置在挑战策略；前端不维护 URL 列表 |

未接全局 412 的旧前端，在启用挑战后会 **无法登录**，敏感接口也会一直 412。

---

## 4. 开关与模式（给联调 / 运维）

不在管理台配置，在 Nacos：

| 键 | dataId | 含义 |
|---|---|---|
| `ingot.security.challenge.enabled` | `in-service-gateway.yml` | false 关闭全部挑战 |
| `ingot.security.challenge.policy.mode` | 同上 | `local` 读 yaml；`remote` 读安全中心快照 |
| 策略列表 | `in-security-gateway.yml` 的 `ingot.security.challenge.policy.*` | 地板；不要在此文件写 enabled/mode |
| `ingot.vc.image.*` | `in-service-gateway.yml` | 滑块引擎，不是触发策略 |

---

## 5. 错误对照（网关）

| HTTP | `code` | 前端 |
|---|---|---|
| 412 | `CHALLENGE_REQUIRED` | 全局拦截，按 §3 用 `data` 动态拉码 / 验码 / 重试 |
| 403 | `FORBIDDEN_BLOCKED` | 封禁，不是验证码问题 |
| 403 | `ACCOUNT_LOCKED` | 账号锁定 |
| 429 | `LIMIT_TOO_MANY` | 限流；无挑战策略时出现 |

---

## 6. 实现对照

| 项 | 位置 |
|---|---|
| 管理 API | `SecurityPolicyAPI` `/platform/security/policy/challenges` |
| 412 组装 | `ChallengeResponses.buildPayload` |
| 拦截 | `ChallengeFilter` |
| 验码签发 | 网关 captcha `check` + `PassTokenStore` |
| 请求头名 | `VCConstants.HEADER_PASS_TOKEN` / `HEADER_SCOPE`（`In-Vc-Pass-Token` / `In-Vc-Scope`） |
