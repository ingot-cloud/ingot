# 访问防护 Platform API — 前端对接文档

> **受众**：安全中心管理台前端  
> **服务**：`ingot-service-security`（经网关 `/security/**` StripPrefix 后访问）  
> **Base Path**：`/platform/security`  
> **鉴权**：Platform 管理员 JWT + 权限码（见各接口）  
> **响应包装**：统一 `R<T>`（`code` / `message` / `data`）

本文档覆盖 L4 change 涉及的全部**管理面写操作** API：既有网关策略（限流/名单/违规升级）与本 change **新增**的登录失败保护策略。Inner Feign 快照接口仅供后端，不在本文档范围。

---

## 1. 通用约定

### 1.1 请求头

| Header | 必填 | 说明 |
|--------|------|------|
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

失败时 `code` 非 `S0200`，`message` 为可读错误信息。

### 1.3 权限码前缀

| 模块 | query | create | update | delete |
|------|-------|--------|--------|--------|
| 网关策略（限流/名单等） | `platform:security:policy:query` | `platform:security:policy:create` | `platform:security:policy:update` | `platform:security:policy:delete` |
| 登录失败保护策略 | `platform:security:access:login-failure:query` | `platform:security:access:login-failure:create` | `platform:security:access:login-failure:update` | `platform:security:access:login-failure:delete` |

### 1.4 策略变更与热更新

任意写操作成功后，后端发布 `SecurityPolicyInvalidationEvent`：

| 域 | 订阅方 | 前端提示 |
|----|--------|----------|
| 网关策略各域 | 网关 SDK | 「规则将在数秒内生效」 |
| `LOGIN_FAILURE_PROTECTION` | Auth / ingot-security-access | 「登录失败保护策略将在数秒内生效」 |

管理台可提供 **「强制刷新」** 按钮，调用 [`POST /platform/security/policy/broadcast-invalidation`](#514-强制全量失效)（网关全域）；登录失败策略保存后自动发域级失效，**无需单独按钮**（可选后续加专用 broadcast）。

### 1.5 前端页面建议结构

```text
安全中心
└── 访问防护
    ├── API 路径分组        → §2.1
    ├── 限流规则            → §2.2
    ├── 黑白名单            → §2.3
    ├── 违规升级            → §2.4
    ├── 登录失败保护        → §3（L4 新增）
    └── （挑战策略 L6 后续） → §2.5 只读预留
```

---

## 2. 网关策略 API（既有，L4 启用 remote 后前端需可配）

前缀：`/platform/security/policy`

### 2.1 API 路径分组

#### GET `/groups`

查询全部分组。

**权限**：`platform:security:policy:query`

**响应 `data`**：`GatewayEndpointGroup[]`

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | long | 主键 |
| `code` | string | 分组编码，唯一 |
| `name` | string | 展示名称 |
| `patternList` | array | `[{ "path": "/auth/token/**", "method": "POST" }]` |
| `enabled` | boolean | 是否启用 |
| `remark` | string | 备注 |

#### POST `/groups` · PUT `/groups`

新增 / 更新分组。Body 同上单条对象；PUT 须带 `id`。

**权限**：create / update

#### DELETE `/groups/{id}`

**权限**：`platform:security:policy:delete`

---

### 2.2 限流规则

#### GET `/rules`

**响应 `data`**：`GatewayRateLimitRule[]`

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | long | 主键 |
| `code` | string | 规则编码，唯一 |
| `groupCode` | string | 关联分组；空则用 `patternList` |
| `patternList` | array | 内联路径，同分组 |
| `dimension` | string | `IP` / `DV`(device) / `UI`(user) / `CL`(client，L4 扩展) |
| `qps` | int | 平均速率 |
| `burst` | int | 突发 |
| `intervalSec` | int | 统计窗口（秒） |
| `controlBehavior` | string | `F` 快速失败 / `Q` 排队 |
| `enabled` | boolean | |
| `priority` | int | 越小越优先 |
| `remark` | string | |

#### POST `/rules` · PUT `/rules` · DELETE `/rules/{id}`

**权限**：同 §2.1

**前端表单校验**：

- `code` 必填，英文+数字+连字符
- `groupCode` 与 `patternList` 至少填一项
- `qps` > 0

---

### 2.3 黑白名单

#### GET `/ip-list`

**响应 `data`**：`GatewayIpList[]`

| 字段 | 类型 | 说明 |
|------|------|------|
| `listType` | string | `B` 黑 / `W` 白 |
| `keyType` | string | `IP` / `DV` / `UI` / `CD`(CIDR) / `UA` / `RF` / `CL`(L4) |
| `keyValue` | string | 匹配值 |
| `reason` | string | 原因 |
| `source` | string | `M` 手工 / `A` 自动 |
| `effectiveAt` | datetime | 生效时间，null=立即 |
| `expiresAt` | datetime | 失效时间，null=永久 |
| `enabled` | boolean | |

#### POST `/ip-list` · PUT `/ip-list` · DELETE `/ip-list/{id}`

**权限**：同 §2.1

---

### 2.4 违规升级（单行全局配置）

#### GET `/violation-escalation`

**响应 `data`**：单对象

| 字段 | 类型 | 默认 | 说明 |
|------|------|------|------|
| `id` | long | 1 | 固定单行 |
| `windowSec` | int | 60 | 违规计数窗口 |
| `blockThreshold` | int | 30 | 窗口内限流拒绝次数阈值 |
| `tempBlockTtlSec` | int | 900 | 临时封禁 TTL |
| `enabled` | boolean | true | |

#### PUT `/violation-escalation`

Body 同上。**权限**：`platform:security:policy:update`

---

### 2.5 挑战策略（L6 前可选只读展示）

#### GET `/challenges` · POST · PUT · DELETE `/challenges/{id}`

L4 **不启用**网关 challenge SDK；管理面 API 已存在，前端可先隐藏或只读。字段见 Swagger `SecurityChallengePolicy`。

---

### 2.6 封禁审计（历史只读）

#### GET `/events?limit=100`

查询 `gateway_blacklist_event` **历史**；L3 后新事件入 `security_event`，本接口仅历史兼容。

---

### 2.7 强制全量失效

#### POST `/broadcast-invalidation`

强制广播 `SecurityPolicyDomain.ALL`，刷新网关全部策略缓存。

**权限**：`platform:security:policy:update`

---

## 3. 登录失败保护策略 API（L4 新增）

前缀：`/platform/security/access/login-failure-policies`

> **语义**：按 **维度** 配置「认证失败次数 → 临时封禁」阈值；与账号 lockout（L2，Member/PMS 服务配置）分离。  
> **执行**：Auth 服务 `mode=remote` 时 Feign 拉取；变更后 `LOGIN_FAILURE_PROTECTION` 域失效广播。

### 3.1 维度枚举 `LoginFailureDimension`

| 值 | 说明 | 封禁 keyType |
|----|------|--------------|
| `IP` | 按客户端 IP | `IP` |
| `DEVICE` | 按 `In-Ca-Sig` 设备指纹 | `DV` |
| `CLIENT` | 按 OAuth2 `client_id` | `CL` |
| `ACCOUNT_IP` | 按 userType+username+IP 组合；达阈值封 IP | `IP` |

### 3.2 GET `/login-failure-policies`

列表查询，固定最多 4 条（每维度一条）。

**权限**：`platform:security:access:login-failure:query`

**响应 `data`**：`LoginFailureProtectionPolicyVO[]`

```json
[
  {
    "id": 1,
    "dimension": "IP",
    "enabled": true,
    "maxAttempts": 50,
    "windowMinutes": 1,
    "blockTtlSec": 3600,
    "blockKeyType": "IP",
    "remark": "同一 IP 1 分钟失败 50 次封 1 小时",
    "updatedAt": "2026-07-29T10:00:00"
  }
]
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | long | 是 | 主键 |
| `dimension` | string | 是 | 枚举 §3.1，**唯一** |
| `enabled` | boolean | 是 | 维度总开关 |
| `maxAttempts` | int | 是 | 窗口内最大失败次数，≥ 1 |
| `windowMinutes` | int | 是 | 滑动窗口（分钟），≥ 1 |
| `blockTtlSec` | int | 是 | 临时封禁时长（秒），≥ 60 |
| `blockKeyType` | string | 是 | 封禁写入维度，默认与 dimension 对应（`ACCOUNT_IP` → 封 `IP`） |
| `remark` | string | 否 | 展示说明 |
| `createdAt` | datetime | — | 只读 |
| `updatedAt` | datetime | — | 只读 |

### 3.3 GET `/login-failure-policies/{dimension}`

按维度查询单条，`dimension` 路径值为 `IP` / `DEVICE` / `CLIENT` / `ACCOUNT_IP`。

**权限**：query

### 3.4 PUT `/login-failure-policies`

更新单条策略（按 `dimension` upsert）。**种子数据 migration 011 预置 4 行**，前端首次进入为编辑而非新增。

**权限**：`platform:security:access:login-failure:update`

**请求 Body**：同 §3.2 单条对象（须含 `dimension`）

**业务规则**：

- 同一 `dimension` 仅一条生效配置
- `enabled=false` 时该维度不计数、不封禁
- 保存成功后后端递增策略版本并广播 `LOGIN_FAILURE_PROTECTION`

### 3.5 POST `/login-failure-policies/reset-defaults`

（可选）将四维恢复为 migration 种子默认值。

**权限**：`platform:security:access:login-failure:update`

### 3.6 前端表单与交互建议

| 维度 | 推荐默认展示 | 表单提示 |
|------|-------------|----------|
| IP | 50 / 1min / 3600s | 「不影响账号锁定，仅封 IP」 |
| DEVICE | 30 / 5min / 1800s | 「需 BFF 传递 In-Ca-Sig」 |
| CLIENT | 100 / 5min / 3600s | 「仅统计带用户名的密码错误」 |
| ACCOUNT_IP | 10 / 5min / 3600s | 「定向撞库防护，达阈值封 IP」 |

**页面逻辑**：

1. 进入页 → `GET /login-failure-policies` 拉四行，Tab 或卡片分维度编辑
2. 保存 → `PUT` 单维度 → Toast「已保存，约 10 秒内生效」
3. 与 **账号保护** 页区分：账号 lockout 仍在 PMS/Member 服务配置（L2），本页只管 IP/设备/Client/账号+IP

---

## 4. 枚举与常量速查（前端字典）

### 4.1 限流维度 `dimension`

| UI 文案 | 值 |
|---------|-----|
| IP | `IP` |
| 设备 | `DV` |
| 用户 | `UI` |
| Client | `CL` |

### 4.2 名单 keyType

| UI 文案 | 值 |
|---------|-----|
| IP | `IP` |
| 设备 | `DV` |
| 用户 ID | `UI` |
| CIDR | `CD` |
| User-Agent | `UA` |
| Referer | `RF` |
| Client | `CL` |

### 4.3 名单 listType

| 值 | 说明 |
|----|------|
| `B` | 黑名单 |
| `W` | 白名单 |

---

## 5. 错误码与边界（前端处理）

| 场景 | 建议 UX |
|------|---------|
| 403 无权限 | 隐藏菜单或展示无权限页 |
| 删除分组被规则引用 | 后端返回业务错误，提示先解绑规则 |
| `code` 重复 | 限流规则 / 分组编码唯一性校验 |
| 保存后策略未生效 | 引导查看网关/Auth 是否 `mode=remote`、服务是否可达 |

---

## 6. OpenAPI / Swagger

实现阶段在 `LoginFailureProtectionAPI` 上补齐 `@Tag` / `@Operation` / `@Schema`，与现有 `SecurityPolicyAPI` 同一 Swagger 分组「安全中心」。

前端可优先从运行环境拉取：`GET /security/v3/api-docs`（以实际网关路由为准）。

---

## 7. 版本

| 版本 | 日期 | 说明 |
|------|------|------|
| 0.1 | 2026-07-29 | L4 change 草案，对齐 DESIGN D5 remote + Platform CRUD |
