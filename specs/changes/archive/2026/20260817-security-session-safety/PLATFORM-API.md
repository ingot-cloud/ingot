# 安全中心 · 会话管理 Platform API

> **受众**：安全中心管理台前端（「安全中心 / 在线用户」页面及其并发策略子页）
> **服务**：`ingot-service-security`（经网关 `/security/**` StripPrefix 后访问）
> **Base Path**：在线会话 `/platform/security/sessions`、并发策略 `/platform/security/session/concurrency-policies`（经网关加 `/security` 前缀）
> **鉴权**：Platform 管理员 JWT + 权限码（见各接口）
> **响应包装**：统一 `R<T>`（`code` / `message` / `data`）
> **状态**：Phase 03 交付会话查询/下线（§2–§7），Phase 04 增加并发策略维护（§8）

⚠️ **本页替代已删除的 Auth `/auth/token/**`**。旧接口（`GET /auth/token/tokens`、`DELETE /auth/token/jti`、`DELETE /auth/token/user`）连同 `TokenEndpoint` 已在 Phase 02 删除，网关也不再注册该路由，**没有兼容层**。迁移映射见 [§5](#5-从旧在线用户页迁移)。

---

## 1. 通用约定

### 1.1 请求头

| Header | 必填 | 说明 |
|--------|------|------|
| `Authorization` | 是 | `Bearer {accessToken}`，平台管理员令牌 |
| `Content-Type` | 否 | 本页无 JSON 请求体，参数一律走 query / path |

### 1.2 响应结构

```json
{ "code": "0", "message": null, "data": {} }
```

`code = "0"` 为成功。失败时 HTTP 状态可能是 500（业务异常）而 `code` 才是判断依据，前端请以 `code` + `message` 提示，不要只看 HTTP 状态码。

### 1.3 权限码

| 权限码 | 覆盖接口 |
|--------|----------|
| `platform:security:session:query` | 会话列表、会话详情 |
| `platform:security:session:revoke` | 按 sid 下线、按用户下线 |
| `platform:security:session:policy:query` | 并发策略列表、详情（§8） |
| `platform:security:session:policy:update` | 并发策略新增、修改、删除（§8） |

菜单权限沿用既有 `platform:security:onlinetoken`；四个操作码分别由 migration `014_session_admin_permission_seed.sql`（会话）与 `017_session_policy_permission_seed.sql`（并发策略）挂在该菜单下。超级管理员无需授权即可访问（`@AdminOrHasAnyAuthority` 的 admin 分支）。

### 1.4 会话身份：`sid` 取代 `jti`

- **`sid`** 是会话唯一标识，创建后不变，**下线操作只认 sid**。
- **`jti`** 是当前 Access Token 的 ID，令牌续期后会变化，只用于和网关/审计日志对照，**不能**作为下线入参。

旧页面按 `jti` 下线的逻辑必须改为按 `sid`。

### 1.5 时间字段格式

`issuedAt` / `expiresAt` / `lastAccessAt` 是 **ISO-8601 UTC 瞬时**，形如 `2026-08-19T03:21:00Z`，与库表实体的 `yyyy-MM-dd HH:mm:ss`（本地时间）不同。前端需 `new Date(str)` 后按本地时区格式化。

`lastAccessAt` 只在**登录与令牌续期**时更新，不是「最后一次业务请求时间」——资源服务器热路径不写 Redis。UI 文案建议用「最近凭据活动」而非「最后活跃」。

### 1.6 查询范围约束（重要）

会话在 Redis 中按「租户 + Client」组织，分页游标建立在该维度的在线用户有序集合上，因此：

| 查询方式 | 是否支持 | `total` 语义 |
|----------|----------|--------------|
| `clientId` | ✅ | **在线用户数**（非会话数）；单会话模式下两者相等，多会话模式下同一页可能返回多于 `size` 条 |
| `clientId` + `userId` | ✅ | 精确会话数 |
| `clientId` + `ipAddress` | ✅ | 精确会话数 |
| `userId`（不给 clientId） | ✅ 跨全部 Client | 精确会话数 |
| 都不给（全租户全 Client 翻页） | ❌ 拒绝 | — |

**前端必须提供 Client 选择器**（或先按用户搜索）。Client 列表可用既有客户端管理接口 `GET /auth/client/page` 获取（`clientId` + 名称），注意该接口属开发者平台域、需要 `platform:develop:client:query` 权限：只有会话权限的管理员拿不到列表，此时应退化为「手填 clientId」或前端内置常用 Client 列表。

---

## 2. 查询接口

### 2.1 GET `/platform/security/sessions`

分页查询在线会话，**一行一个会话**。

**权限**：`platform:security:session:query`

**Query 参数**

| 参数 | 类型 | 必填 | 默认 | 说明 |
|------|------|------|------|------|
| `tenantId` | long | 否 | 当前登录管理员所属租户 | 租户 ID |
| `clientId` | string | 条件必填 | — | OAuth2 Client；未传 `userId` 时必填 |
| `userId` | long | 条件必填 | — | 按用户过滤；未传 `clientId` 时必填，此时跨全部 Client |
| `ipAddress` | string | 否 | — | 按登录 IP 过滤，需与 `clientId` 同时给出 |
| `current` | long | 否 | `1` | 页码，从 1 开始 |
| `size` | long | 否 | `20` | 每页条数，上限 `200`，超出按 200 处理 |

**响应 `data`**：`IPage<PlatformSessionVO>`

```json
{
  "code": "0",
  "data": {
    "current": 1,
    "size": 20,
    "total": 3,
    "pages": 1,
    "records": [
      {
        "sid": "8f1c2f6e-6a3d-4a2e-9c1b-2f5f6a7b8c9d",
        "jti": "3c9a1d55-77bb-4c0e-9d3a-1b2c3d4e5f60",
        "userId": 1,
        "username": "admin",
        "nickname": "超级管理员",
        "avatar": "https://oss.example.com/avatar/1.png",
        "tenantId": 1,
        "tenantName": "平台租户",
        "clientId": "web",
        "authType": "1",
        "userType": "0",
        "ipAddress": "10.0.0.7",
        "location": "浙江杭州",
        "deviceType": "PC",
        "os": "macOS",
        "browser": "Chrome",
        "userAgent": "Mozilla/5.0 ...",
        "issuedAt": "2026-08-19T03:21:00Z",
        "expiresAt": "2026-08-26T03:21:00Z",
        "lastAccessAt": "2026-08-19T06:02:11Z"
      }
    ]
  }
}
```

**`PlatformSessionVO` 字段**

| 字段 | 类型 | 可空 | 说明 |
|------|------|------|------|
| `sid` | string | 否 | 会话 ID，下线入参 |
| `jti` | string | 是 | 当前 Access Token ID，续期后变化 |
| `userId` | long | 否 | 用户 ID |
| `username` | string | 否 | 登录账号名；PMS 可用时为 PMS 账号名，否则回落会话内的登录名 |
| `nickname` | string | 是 | 昵称；PMS 不可用或 C 端用户时为空 |
| `avatar` | string | 是 | 头像；同上 |
| `tenantId` | long | 否 | 租户 ID |
| `tenantName` | string | 是 | 租户名称；PMS 不可用时为空 |
| `clientId` | string | 否 | OAuth2 Client |
| `authType` | string | 是 | 并发策略，`0` 多会话 / `1` 单会话，见 §4.1 |
| `userType` | string | 是 | `0` 管理用户 / `1` C 端用户，见 §4.2 |
| `ipAddress` | string | 是 | 登录 IP |
| `location` | string | 是 | 登录地理位置，未解析时为空 |
| `deviceType` | string | 是 | 设备类型 |
| `os` | string | 是 | 操作系统 |
| `browser` | string | 是 | 浏览器 |
| `userAgent` | string | 是 | 原始 UA，建议详情页再展示 |
| `issuedAt` | string | 是 | 会话创建时间（ISO-8601 UTC），续期不改写 |
| `expiresAt` | string | 是 | 会话过期时间，对齐 Refresh Token 剩余寿命 |
| `lastAccessAt` | string | 是 | 最近一次登录/续期时间，见 §1.5 |

**名称降级**：PMS 不可达时 `nickname` / `avatar` / `tenantName` 为空、`username` 回落为会话内登录名，**其余字段与下线能力不受影响**。前端在名称为空时展示 `userId` 兜底即可，不要因为名称缺失禁用下线按钮。

### 2.2 GET `/platform/security/sessions/{sid}`

会话详情。

**权限**：`platform:security:session:query`

**Path 参数**：`sid`

**响应 `data`**：`PlatformSessionVO`；会话已过期或已被撤销时返回 `code = "0"` 且 `data = null`（**不是**错误），前端应提示「会话已不存在」并刷新列表。

---

## 3. 下线接口

两个下线接口都用 `DELETE` + query 参数（不用请求体，规避部分网关/浏览器丢弃 DELETE body）。

**原因与操作者由后端固定**：`reason = ADMIN_REVOKE`、操作者取当前登录管理员，前端**不需要也不能**传 `reason` / `actorId`，避免安全事件被伪造。下线会产生 `SESSION_REVOKED` 安全事件（可在安全事件页查询）。

### 3.1 DELETE `/platform/security/sessions/{sid}`

强制下线单个会话。

**权限**：`platform:security:session:revoke`

**Path 参数**：`sid`

**响应 `data`**：`boolean`
- `true`：确实撤销了一个存活会话
- `false`：该会话已不在线（幂等，不是错误）——前端提示「会话已不在线」并刷新列表

### 3.2 DELETE `/platform/security/sessions/user`

强制下线某用户的会话。

**权限**：`platform:security:session:revoke`

**Query 参数**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `tenantId` | long | 否 | 为空取当前登录管理员所属租户 |
| `userId` | long | **是** | 目标用户 |
| `clientId` | string | 否 | 留空表示下线该用户在该租户下**全部 Client** 的会话 |

**响应 `data`**：`int`，实际撤销的会话数（`0` 表示该用户当前无在线会话）。

**生效时机**：撤销后目标用户的 Access Token 下一次请求即被拒绝（资源服务器按 sid 查会话，查不到即视为已撤销），Refresh Token 也无法再换新令牌，无需等待令牌过期。

---

## 4. 枚举速查

### 4.1 `authType`（Client 上的并发缺省值，对应 `TokenAuthTypeEnum`）

| 值 | 枚举 | 说明 |
|----|------|------|
| `0` | `STANDARD` | 多会话：允许并存，不互踢 |
| `1` | `UNIQUE` | 单会话：同一用户同一 Client 新登录会踢掉旧会话 |

Phase 04 起 `authType` 只是**缺省值**：并发策略（§8）给出显式 `maxSessions` 时以策略为准，`UNIQUE` 仅在策略为「不限制」时按单会话处理。会话列表里的该字段来自 Client 配置，不代表实际生效的策略，UI 上建议标注「客户端缺省」。

### 4.2 `userType`（对应 `UserTypeEnum`）

| 值 | 枚举 | 说明 |
|----|------|------|
| `0` | `ADMIN` | 管理用户（PMS 用户，名称可补全） |
| `1` | `APP` | C 端用户（不在 PMS 用户表，`nickname` / `avatar` 恒为空） |

### 4.3 `deviceType`

由 User-Agent 解析归一：`PC`、`Mobile`、`Tablet`、`Bot`、`Other`、`Unknown`；无法归类时回落解析器原始设备类别。前端按未知值兜底展示原文即可。

---

## 5. 从旧「在线用户」页迁移

### 5.1 接口映射

| 旧（已删除） | 新 | 差异 |
|--------------|----|------|
| `GET /auth/token/tokens?tenantId&clientId&current&size` | `GET /security/platform/security/sessions?tenantId&clientId&current&size` | 返回**扁平会话列表**，不再是「用户行 + `tokens[]`」嵌套 |
| `DELETE /auth/token/jti`，body `{ "payload": "{jti}" }` | `DELETE /security/platform/security/sessions/{sid}` | 入参 `jti` → `sid`，改走 path 参数 |
| `DELETE /auth/token/user`，body `{ userId, tenantId, clientId }` | `DELETE /security/platform/security/sessions/user?userId&tenantId&clientId` | 参数从 body 改为 query |
| — | `GET /security/platform/security/sessions/{sid}` | 新增详情接口 |

### 5.2 字段映射

| 旧 `UserTokenVO` | 新 `PlatformSessionVO` |
|------------------|------------------------|
| `userId` / `nickname` / `avatar` | 同名字段（每条会话上都有） |
| `tenantName` | `tenantName` |
| `tenantLogo` | 已移除（列表按单租户查询，logo 无信息量） |
| `clientName` | 已移除，改用 `clientId`；名称由前端 Client 选择器自带 |
| `tokens[]`（`OnlineToken` 数组） | 会话字段直接平铺到记录上；`tokens[].jti` → `sid`（下线用）+ `jti`（仅展示） |

### 5.3 前端改造清单

1. **接口地址与权限**：请求前缀由 `/auth/token` 改为 `/security/platform/security/sessions`；按钮权限码改为 `platform:security:session:query` / `platform:security:session:revoke`。
2. **表格结构**：由「用户行可展开会话」改为「一行一个会话」。若要保留原视觉，可在前端按 `userId` 客户端分组，但**下线按钮必须绑定到 sid**。
3. **必选 Client**：列表页需要 Client 选择器（`GET /auth/client/page`，需 `platform:develop:client:query`；无该权限时退化为手填或内置列表），或提供「按用户搜索」入口（只填 `userId` 即可跨 Client 查询）。
4. **分页提示**：仅按 Client 查询（无 `userId` / `ipAddress`）时 `total` 是在线用户数，多会话模式下当页条数可能大于 `size`；建议在分页组件旁标注「按在线用户翻页」，或默认按用户/IP 过滤以获得精确计数。
5. **下线交互**：按 sid 下线返回 `false` 表示会话已不在线，按提示刷新即可，不当作失败；按用户下线返回撤销数量，建议提示「已下线 N 个会话」。
6. **时间展示**：ISO-8601 UTC → 本地时区格式化；`lastAccessAt` 文案改为「最近凭据活动」。
7. **名称缺失兜底**：`nickname` / `tenantName` 为空时展示 `userId` / `tenantId`，不影响操作按钮可用性。
8. **新增筛选项（可选）**：`ipAddress` 支持按登录 IP 排查异常会话（需同时选定 Client）。
9. **页面路由不变**：菜单仍是「安全中心 / 在线用户」（`/platform/security/onlinetoken`），无需新增菜单或改路由，只改页面内的接口与交互。

---

## 6. 错误与边界

| 场景 | 响应 | 建议 UX |
|------|------|---------|
| 未选 Client 也未填 userId | `code = "S0002"`，message 含「查询在线会话必须指定 clientId 或 userId」 | 表单校验前置拦截，不要发请求 |
| 按用户下线未填 userId | `code = "S0002"`，message 含「按用户下线必须指定 userId」 | 同上 |
| 无 `tenantId` 且上下文缺租户 | `code = "S0002"`，message 含「查询在线会话必须指定 tenantId」 | 提示重新登录或显式选择租户 |
| 权限不足 | HTTP 403 | 隐藏/禁用对应按钮 |
| Auth 服务不可达 | `code` 为 Feign/系统错误码 | 明确提示「会话服务不可用」，**不要**展示为「无在线会话」 |
| PMS 不可达 | `code = "0"`，名称字段为空 | 列表照常展示，名称位置兜底 ID |
| 查询详情返回 `data = null` | `code = "0"` | 提示「会话已不存在」并刷新列表 |
| 下线返回 `false` | `code = "0"` | 提示「会话已不在线」并刷新列表 |

---

## 7. OpenAPI / Swagger

运行环境可取实时文档：`GET /security/v3/api-docs`（Tag：`安全中心-在线会话`、`安全中心`）。本文档与 `PlatformSessionAPI`、`PlatformSessionVO`、`PlatformSessionQueryDTO`、`PlatformUserSessionRevokeDTO`、`SessionConcurrencyPolicyAPI`、`SessionConcurrencyPolicy` 同步维护，字段以代码为准。

---

## 8. 并发会话策略（Phase 04 新增）

**Base Path**：`/platform/security/session/concurrency-policies`（经网关即 `/security/platform/security/session/concurrency-policies`）

一条策略回答两个问题：**同一用户在同一 Client 上最多能有几个会话**，以及**超出时怎么处置**。策略集中在安全中心维护，Auth 各实例经内部接口 + 分层缓存读取，保存后**不需要重启**即在下一次登录生效。

### 8.1 生效模型（前端必须向用户讲清的三点）

1. **匹配是「命中即止」，不做字段合并**：优先级 `CLIENT` > `USER_TYPE` > `GLOBAL`。某 Client 有策略时，`USER_TYPE` 与 `GLOBAL` 完全不参与，不会「继承」其中的 `overflow` 或 `adminForbidConcurrent`。
2. **只影响「新登录」**：策略调整不会去动已经在线的会话，也不会立刻踢人。收敛发生在受影响用户下一次登录时。需要立刻清场请配合 §3 的强制下线。
3. **令牌续期不受策略约束**：refresh 只是延长既有会话，不触发并发判定。

### 8.2 GET `/platform/security/session/concurrency-policies`

策略列表，**无分页**（策略条数量级为个位数到十几条），按 `scope` → `clientId` → `userType` 升序。

**权限**：`platform:security:session:policy:query`

**响应 `data`**：`SessionConcurrencyPolicy[]`

```json
{
  "code": "0",
  "data": [
    {
      "id": 1,
      "scope": "GLOBAL",
      "clientId": "",
      "userType": "",
      "maxSessions": 0,
      "dimension": "USER_CLIENT",
      "overflow": "KICK_OLDEST",
      "adminForbidConcurrent": false,
      "enabled": true,
      "remark": "全局兜底：不限制并发会话",
      "createdAt": "2026-08-19 10:00:00",
      "updatedAt": "2026-08-19 10:00:00"
    },
    {
      "id": 2,
      "scope": "CLIENT",
      "clientId": "web",
      "userType": "",
      "maxSessions": 1,
      "dimension": "USER_CLIENT",
      "overflow": "KICK_OLDEST",
      "adminForbidConcurrent": false,
      "enabled": true,
      "remark": "管理台 Web 端单会话",
      "createdAt": "2026-08-19 10:05:00",
      "updatedAt": "2026-08-19 10:05:00"
    }
  ]
}
```

**字段**

| 字段 | 类型 | 必填（写入时） | 说明 |
|------|------|----------------|------|
| `id` | long | 更新必填 | 主键；新增时不传（传了也会被忽略） |
| `scope` | string | 是 | `GLOBAL` / `CLIENT` / `USER_TYPE`，见 §8.6 |
| `clientId` | string | `scope=CLIENT` 时必填 | 其余 scope 后端一律落空串，前端传了也会被清掉 |
| `userType` | string | `scope=USER_TYPE` 时必填 | 取值同 §4.2（`0` / `1`）；其余 scope 落空串 |
| `maxSessions` | int | 是 | 最大并发会话数，`0` = 不限制；不接受负数 |
| `dimension` | string | 否 | 目前仅 `USER_CLIENT`（同一用户 + 同一 Client 计数），缺省即此值 |
| `overflow` | string | 否 | `REJECT` / `KICK_OLDEST` / `KICK_ALL`，缺省 `KICK_OLDEST`，见 §8.7 |
| `adminForbidConcurrent` | boolean | 否 | `true` 时管理用户（`userType=0`）强制单会话，覆盖 `maxSessions`；缺省 `false` |
| `enabled` | boolean | 否 | 缺省 `true`；`false` 的策略在匹配时被跳过，等价于「不存在」 |
| `remark` | string | 否 | 备注，≤255 字 |
| `createdAt` / `updatedAt` | string | — | 只读，格式 `yyyy-MM-dd HH:mm:ss`（**本地时间**，与 §1.5 会话时间的 ISO-8601 UTC 不同，别复用同一个格式化函数） |

### 8.3 GET `/platform/security/session/concurrency-policies/{id}`

策略详情。**权限**：`platform:security:session:policy:query`。

**响应 `data`**：`SessionConcurrencyPolicy`；`id` 不存在时返回业务错误（见 §8.8），不是 `data = null`。

### 8.4 POST `/platform/security/session/concurrency-policies`

新增策略。**权限**：`platform:security:session:policy:update`。

**请求体**：`SessionConcurrencyPolicy`（字段见 §8.2；`id` / `createdAt` / `updatedAt` 由后端接管）

```json
{
  "scope": "CLIENT",
  "clientId": "web",
  "maxSessions": 1,
  "overflow": "KICK_OLDEST",
  "adminForbidConcurrent": false,
  "enabled": true,
  "remark": "管理台 Web 端单会话"
}
```

**响应 `data`**：落库后的完整策略（含 `id` 与补齐的缺省值），前端可直接用它更新列表，无需重新拉取。

同一 `(scope, clientId, userType)` 只允许一条策略，重复提交返回「相同生效范围的并发会话策略已存在」。

### 8.5 PUT / DELETE

| 方法 | 路径 | 说明 |
|------|------|------|
| `PUT` | `/platform/security/session/concurrency-policies` | 全量更新，**请求体必须带 `id`**；未传的可选字段会被填成缺省值而不是保持原值，编辑表单请回填完整对象后再提交 |
| `DELETE` | `/platform/security/session/concurrency-policies/{id}` | 删除；`data` 为 `null`（`code = "0"` 即成功） |

`scope=GLOBAL` 的兜底策略**不允许删除**（后端拒绝）。要取消全局限制请把 `maxSessions` 改成 `0`。前端应对 GLOBAL 行禁用删除按钮并给出该提示，而不是等后端报错。

### 8.6 `scope` 枚举

| 值 | 含义 | 需填字段 | 说明 |
|----|------|----------|------|
| `GLOBAL` | 全局兜底 | — | 有且仅有一条，不可删除 |
| `CLIENT` | 按 OAuth2 Client | `clientId` | 最高优先级；Client 列表来源同 §1.6 |
| `USER_TYPE` | 按用户类型 | `userType` | 取值 `0` 管理用户 / `1` C 端用户 |

### 8.7 `overflow` 枚举

| 值 | 行为 | 用户感知 |
|----|------|----------|
| `REJECT` | 拒绝本次新登录，旧会话全部保留 | 新设备登录报错（错误码 `concurrent_session_limit`），需先在旧设备退出 |
| `KICK_OLDEST` | 按会话创建时间踢最旧的，直到腾出名额 | 新设备登录成功，最早登录的设备被下线 |
| `KICK_ALL` | 踢掉该维度下全部旧会话 | 新设备登录成功，其余设备全部下线 |

被踢会话会产生 `SESSION_CONCURRENT_KICKOUT` 安全事件（可在安全事件页查询）；`REJECT` 时登录接口返回 OAuth2 错误响应，前端应按「并发会话超限」文案提示，而不是笼统的「登录失败」。

### 8.8 错误与边界

| 场景 | message（`code` 为业务错误码） | 建议 UX |
|------|------------------------------|---------|
| `scope` 为空 | 策略生效范围不能为空 | 表单必填校验前置 |
| `scope=CLIENT` 未填 `clientId` | 按客户端生效的策略必须指定客户端 | 同上，选择 scope 后动态切换必填项 |
| `scope=USER_TYPE` 的 `userType` 非法 | 按用户类型生效的策略必须指定合法的用户类型 | 用下拉而非输入框 |
| `maxSessions` < 0 | 最大并发会话数不能小于 0 | 数字输入框限制最小值 0 |
| 范围重复 | 相同生效范围的并发会话策略已存在 | 提示「该范围已有策略，请直接编辑」 |
| 删除 GLOBAL | 全局兜底策略不可删除，可将最大会话数改为 0 以关闭限制 | 前置禁用删除按钮 |
| `id` 不存在 | 并发会话策略不存在 | 刷新列表 |
| 权限不足 | HTTP 403 | 隐藏/禁用维护类按钮 |

### 8.9 前端改造清单（并发策略子页）

1. **入口**：挂在既有「安全中心 / 在线用户」页面下（Tab 或子页均可），无需新增菜单与权限节点；维护类按钮按 `platform:security:session:policy:update` 控制显隐。
2. **列表**：无分页，直接渲染数组。建议把 GLOBAL 行固定置顶并视觉区分（不可删除）。
3. **表单联动**：`scope` 切换时动态显示 `clientId` / `userType`，并清空另一个；不要把用户填过的值一起提交（后端会清，但会造成「提交后字段消失」的困惑）。
4. **`maxSessions=0` 文案**：展示为「不限制」而非「0」，否则容易被读成「禁止登录」。
5. **编辑用全量提交**：`PUT` 是全量覆盖，编辑弹窗必须先回填完整对象。
6. **生效说明**：保存成功后提示「已保存，下次登录生效；已在线会话不受影响」，避免用户误以为会立刻踢人（需要立刻清场时引导到会话列表手动下线）。
7. **时间格式**：策略的 `createdAt` / `updatedAt` 是 `yyyy-MM-dd HH:mm:ss` 本地时间，直接展示即可，**不要**走会话列表那套 UTC 转换。
8. **登录侧文案**：登录请求收到 `concurrent_session_limit` 时提示「当前账号在线设备数已达上限，请先退出其他设备」；收到 `session_policy_unavailable` 时提示「会话策略暂不可用，请稍后重试」（这是策略服务异常时的 fail-closed 保护，不是账号问题）。

---

## 9. 版本

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0 | 2026-08-19 | Phase 03 首版：会话列表/详情/按 sid 下线/按用户下线，含旧 `/auth/token/**` 迁移映射 |
| 1.1 | 2026-08-19 | Phase 04：新增并发会话策略 CRUD（§8）；`authType` 降级为 Client 缺省值说明 |
