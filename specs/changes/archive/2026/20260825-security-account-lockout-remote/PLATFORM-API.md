# 安全中心 · 账号锁定策略 Platform API

> **受众**：安全中心管理台前端（账号锁定策略页）
> **服务**：`ingot-service-security`（经网关 `/security/**` StripPrefix 后访问）
> **Base Path**：`/platform/security/account/lockout-policies`（经网关加 `/security` 前缀）
> **鉴权**：Platform 管理员 JWT + 权限码（见各接口）
> **响应包装**：统一 `R<T>`（`code` / `message` / `data`）
> **状态**：本 change 新增

Inner Feign（`/inner/security/account/lockout-policies`）仅供 PMS / Member 拉策略，**不要**在管理台调用。

---

## 1. 通用约定

### 1.1 请求头

| Header | 必填 | 说明 |
|--------|------|------|
| `Authorization` | 是 | `Bearer {accessToken}`，平台管理员令牌 |
| `Content-Type` | PUT | `application/json` |

### 1.2 响应结构

```json
{ "code": "0", "message": null, "data": {} }
```

`code = "0"` 为成功。失败时请以 `code` + `message` 提示，不要只看 HTTP 状态码。

### 1.3 权限码

| 权限码 | 覆盖接口 |
|--------|----------|
| `platform:security:account:lockout:query` | 列表、按用户类型查询 |
| `platform:security:account:lockout:update` | 更新（upsert） |

超级管理员走 `@AdminOrHasAnyAuthority` 的 admin 短路，无需单独授权。

菜单建议挂在「安全中心」下，独立「账号锁定」页。种子只有两行，**不要提供删除按钮**。

### 1.4 用户类型

| `userType` | 含义 | 锁定时长约束 |
|------------|------|----------------|
| `"0"` | B 端管理员（PMS / ADMIN） | 允许 `lockDurationMinutes = 0`（永久锁） |
| `"1"` | C 端用户（Member / APP） | **禁止** `0`，须 ≥ 1 |

JSON 与路径均使用上述数字字符串（与全站 `UserTypeEnum` 的 `@JsonValue` 一致）。不要传 `ADMIN` / `APP`。

### 1.5 策略变更与热更新

PUT 成功后后端广播 `ACCOUNT_LOCKOUT` 失效。PMS / Member 在 `mode=remote` 时于数秒内（最多一个 L1 TTL，默认 5 分钟，通常立即）使用新值。

前端保存成功后提示：**「账号锁定策略将在数秒内生效」**。无需单独「强制刷新」按钮。

已处于锁定中的账号**不会**因策略变更自动解锁；新阈值只影响此后的失败计数与新触发的自动锁定。

---

## 2. 字段说明

响应 / 请求体共用同一对象：

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | number | 主键；更新时可不传，以后端按 `userType` 定位为准 |
| `userType` | string | `"0"` 或 `"1"`，不可改为另一类型 |
| `enabled` | boolean | 是否启用自动锁定 |
| `maxAttempts` | number | 失败次数阈值，≥ 1 |
| `lockDurationMinutes` | number | 锁定时长（分钟）；`0`=永久，仅 `userType=0` 允许 |
| `attemptWindowMinutes` | number | 失败计数滑动窗口（分钟），≥ 1 |
| `hintAfterAttempts` | number | 从第几次失败开始给出剩余次数提示，1 ≤ 值 ≤ `maxAttempts` |
| `remark` | string \| null | 备注 |
| `createdAt` | string | 创建时间 |
| `updatedAt` | string | 更新时间 |

种子默认值：

| userType | enabled | maxAttempts | lockDurationMinutes | attemptWindowMinutes | hintAfterAttempts |
|----------|---------|-------------|---------------------|----------------------|-------------------|
| `"0"` ADMIN | true | 5 | 30 | 15 | 3 |
| `"1"` APP | true | 5 | 15 | 15 | 3 |

---

## 3. 接口

### 3.1 GET `/platform/security/account/lockout-policies`

查询全部账号锁定策略（固定 2 行）。

**权限**：`platform:security:account:lockout:query`

**响应 `data`**：`AccountLockoutPolicy[]`

```json
{
  "code": "0",
  "message": null,
  "data": [
    {
      "id": 1,
      "userType": "0",
      "enabled": true,
      "maxAttempts": 5,
      "lockDurationMinutes": 30,
      "attemptWindowMinutes": 15,
      "hintAfterAttempts": 3,
      "remark": "B端管理员登录失败锁定",
      "createdAt": "2026-08-25T00:00:00",
      "updatedAt": "2026-08-25T00:00:00"
    },
    {
      "id": 2,
      "userType": "1",
      "enabled": true,
      "maxAttempts": 5,
      "lockDurationMinutes": 15,
      "attemptWindowMinutes": 15,
      "hintAfterAttempts": 3,
      "remark": "C端用户登录失败锁定",
      "createdAt": "2026-08-25T00:00:00",
      "updatedAt": "2026-08-25T00:00:00"
    }
  ]
}
```

### 3.2 GET `/platform/security/account/lockout-policies/{userType}`

按用户类型查询一行。

**权限**：`platform:security:account:lockout:query`

**Path**

| 参数 | 说明 |
|------|------|
| `userType` | `"0"` 或 `"1"` |

**响应 `data`**：单个策略对象；不存在时 `data` 为 `null`。

非法 `userType` 返回业务错误，`message` 为「用户类型不能为空或取值非法」。

### 3.3 PUT `/platform/security/account/lockout-policies`

按 `userType` upsert（种子已存在，前端按更新使用）。

**权限**：`platform:security:account:lockout:update`

**请求体示例**

```json
{
  "userType": "1",
  "enabled": true,
  "maxAttempts": 5,
  "lockDurationMinutes": 15,
  "attemptWindowMinutes": 15,
  "hintAfterAttempts": 3,
  "remark": "C端用户登录失败锁定"
}
```

**校验失败 `message`（节选）**

| 场景 | message |
|------|---------|
| 体为空 | 账号锁定策略不能为空 |
| userType 非法 | 用户类型不能为空或取值非法 |
| maxAttempts < 1 | 最大失败次数不能小于 1 |
| attemptWindowMinutes < 1 | 失败计数窗口不能小于 1 分钟 |
| hintAfterAttempts 非法 | 提示起始次数必须在 1 与失败阈值之间 |
| APP 且 lockDurationMinutes = 0 | C端账号禁止永久自动锁定 |

成功响应 `data` 为更新后的完整对象。前端提示「账号锁定策略将在数秒内生效」。

---

## 4. 页面建议

```text
安全中心
└── 账号锁定
    ├── B 端（管理员）    → userType=0，可勾选「永久锁定」
    └── C 端（会员）      → userType=1，隐藏或禁用「永久锁定」
```

两张表单或左右两栏均可；提交时分别 PUT，不要合并成一次请求。不要提供新增 / 删除。
