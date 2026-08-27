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

登录产品页（非安全中心）：账号密码提交必须实现 §3 的 412 重试，不能再依赖「登录请求带验证码」。

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

## 3. 客户端：412 → 滑块 → 重试

执行者不在安全中心服务，而在 **网关**。浏览器只与网关公网入口交互。

### 3.1 何时会出现 412

| 条件 | 结果 |
|---|---|
| `ingot.security.challenge.enabled=false` | 不会 412 |
| 静态白名单命中 | 不会 412 |
| 黑名单 / 临时封禁 | **403** `FORBIDDEN_BLOCKED`，先于挑战 |
| 匹配 `ALWAYS` 且无有效 `_vc_pass_token` | **412** `CHALLENGE_REQUIRED` |
| Sentinel 限流且匹配 `ON_RATE_LIMIT` | **412** |
| Sentinel 限流且无挑战策略 | **429** `LIMIT_TOO_MANY` |

登录（种子启用后）走第一行 ALWAYS。

### 3.2 412 响应

HTTP **412 Precondition Failed**。

```json
{
  "code": "CHALLENGE_REQUIRED",
  "msg": "Captcha required",
  "data": {
    "vcType": "image",
    "scope": "login",
    "scopeParam": "_vc_scope",
    "passTokenParam": "_vc_pass_token",
    "checkPath": "/vc/image/check",
    "ttlSec": 300,
    "remaining": 3
  }
}
```

| 字段 | 前端用法 |
|---|---|
| `vcType` | 固定按 `image` 拉码：`GET /vc/{vcType}` |
| `scope` | 验码时原样作为 `scopeParam` 的值 |
| `scopeParam` | 查询参数名，值为 `_vc_scope` |
| `passTokenParam` | 重试业务请求时的查询参数名，值为 `_vc_pass_token` |
| `checkPath` | 验码路径，值为 `/vc/image/check` |
| `ttlSec` / `remaining` | 展示用；以服务端签发为准 |

Header：`WWW-Authenticate: Captcha realm="image"`。

**不要**使用 `data.vcType=slider` 去请求 `/vc/slider/**`（该路由不存在）。

### 3.3 推荐时序

```text
1. POST /bff/auth/login          JSON { username, password }   → 412
2. GET  /vc/image                拉拼图（anji get）
3. 用户完成滑块
4. POST /vc/image/check?_vc_scope={data.scope}
     body/query：anji 要求的 pointJson、token
5. 从响应 data 取 _vc_pass_token
6. POST /bff/auth/login?_vc_pass_token={token}&_vc_scope={data.scope}
     JSON 仍为 { username, password }
7. 200 则进入原登录后续（选租户等）；token 失效则回到 1
```

PassToken **与 scope** 必须出现在 **第 6 步的 query**，不要放进 BFF JSON——`BffLoginDTO` 没有验证码字段。只带 token、不带 `_vc_scope` 时，限流挑战路径 **无法跳过 Sentinel**（L6 修复后的明确行为）。

### 3.4 拉码 `GET /vc/image`

- 无需登录。
- 响应为 anji `ResponseModel`（含拼图 token、底图等），前端组件与既有 anji-plus 滑块 SDK 对齐即可。
- 频率受网关 `ingot.vc.image.opsLimitGetPerMinute` 限制，超限返回验证码模块错误，不是 412。

### 3.5 验码 `POST /vc/image/check`

| 参数 | 位置 | 说明 |
|---|---|---|
| `_vc_scope` | query | **必须**，等于 412 的 `data.scope` |
| `token` | query 或表单 | anji 拉码返回的 token |
| `pointJson` | query 或表单 | 滑块轨迹 / 坐标（anji 约定） |

**成功（已带合法 scope）**：

```json
{
  "code": "S0200",
  "data": {
    "captcha": { },
    "_vc_pass_token": "无连字符 hex/uuid",
    "_vc_scope": "login"
  }
}
```

前端以 `data._vc_pass_token` 为准。不要再用 `captchaVerification` 去登录。

**失败**：滑块错误、scope 无对应策略、Redis 无法签发。此时 **不要** 重试业务请求。

未带 `_vc_scope`：可能只返回 anji 结果且 **没有** PassToken，登录仍会 412。挑战场景必须带 scope。

### 3.6 重试业务请求

原方法、原 path、原 body/header 保持不变，增加：

```text
?_vc_pass_token=<data._vc_pass_token>&_vc_scope=<data.scope>
```

`_vc_scope` 必须与 412 / check 响应中的 scope **完全一致**（例如限流策略常用 `anon`，不是 `default`）。若原 URL 已有 query，用 `&` 拼接。`remaining` 次内可重复消费；登录建议每次登录拿新 token。

错误 token、过期、错误 scope → 再次 412，重新走 3.3。

### 3.7 与旧模型的差异（破坏性）

| 旧（`verifyUrls`） | 新（L6） |
|---|---|
| 登录请求带 `_vc_code` / captchaVerification | 登录请求带 `_vc_pass_token` |
| 网关 `checkOnly` 拦登录 | 网关先 412，验码在 `/vc/image/check` |
| 配置在 `ingot.vc.verifyUrls`（仅 Nacos） | 配置在挑战策略（中心或 Nacos） |

旧前端不改会在启用挑战后 **无法登录**（一直 412）。

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
| 412 | `CHALLENGE_REQUIRED` | 弹滑块，按 §3 |
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
| 查询参数名 | `VCConstants.QUERY_PARAMS_PASS_TOKEN` / `QUERY_PARAMS_SCOPE` |
