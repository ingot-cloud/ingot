# 账号保护 SPEC

> 记录当前已验收并在线生效的系统事实。

## 1. 策略配置（`ingot.security.account`）

统一开关：`ingot.security.account.mode = local | remote`（默认 `local`）。

- `local`：每次即时读 Nacos `lockout.*`。
- `remote`：`L1 → L2 → Feign → LKG → Nacos 地板`；空列表视为远端不可用，不 fail-open。`mode=remote` 但 Loader 未装配时启动失败，不再静默回退 local。

| 配置分组 | 关键属性 | 说明 |
|---|---|---|
| `lockout.*` | `enabled` / `maxAttempts` / `lockDurationMinutes` / `attemptWindowMinutes` / `hintAfterAttempts` | local 生效源；remote 时作 Nacos 地板 |
| `policy.fallback.local-floor-enabled` | 默认 `true` | 无 LKG 时是否落地板 |
| `policy.cache.*` | L1 5min / L2 30min | 仅 remote 使用 |

默认值（代码 `@ConfigurationProperties`）：`enabled=true`、`maxAttempts=5`、`lockDurationMinutes=30`、`attemptWindowMinutes=15`、`hintAfterAttempts=3`。

**B/C 差异**：

- `mode=local`：仍由服务级 Nacos 表达。PMS 推荐 5 / 30 / 3（允许永久锁）；Member 推荐 5 / 15 / 3。现网 Member Nacos 地板仍可能为 30 分钟。
- `mode=remote`：安全中心 `account_lockout_policy_config` 按 `user_type` 两行。ADMIN 种子 5 / 30min / 15 / 3；APP 种子 5 / 15min / 15 / 3，管理面禁止 APP `lockDurationMinutes=0`。

旧前缀 `ingot.account.*` 已废弃，代码不再读取。

## 2. 策略加载 seam

消费侧（`RecordLoginUseCaseService`、`AuthContextSupport`）**只经** `AccountLockoutPolicyLoader.getLockoutPolicy(UserTypeEnum)` 取不可变 `LockoutPolicy` 值对象，不直读 `@ConfigurationProperties`。

- `LocalAccountLockoutPolicyLoader`：`mode=local` 每次调用即时从 `AccountDomainProperties` 映射，无进程内缓存；Nacos 经 `ConfigurationPropertiesRebinder` 重绑定后下次读取即生效。
- `CachedAccountLockoutPolicyLoader`（account-adapter）：`mode=remote` 走 `ingot-cache` 链，从全量快照按 `userType` 取行；缺行回落快照第一行（地板单元素）。
- 缓存名 `account-lockout-policy`；L2 `in:sec:account:policy:snapshot`；LKG `in:sec:account:policy:lkg`。失效域 `SecurityPolicyDomain.ACCOUNT_LOCKOUT`。Actuator `GET /actuator/accountlockoutpolicy`。
- 安全中心：表 `account_lockout_policy_config`（migration `018`）；Platform `/platform/security/account/lockout-policies`；Inner `/inner/security/account/lockout-policies`；前端契约见 change 内 `PLATFORM-API.md`。

**策略 vs 用户数据分离**：seam 仅覆盖 lockout 策略参数；`account_lock_state` 锁定状态与失败计数永远落 DB，不进入 remote/LKG/缓存链。

## 3. 数据模型

锁定态表结构 ADMIN / Member **完全一致**，分库部署；安全事件写入各库 canonical `security_event`（migration `012`）：

| 库 | 锁定态表 | 事件表（权威） | `user_type` |
|---|---|---|---|
| `ingot_core` | `account_lock_state` | `security_event` | `0`（ADMIN） |
| `ingot_member` | `account_lock_state` | `security_event` | `1`（APP） |

- `account_lock_state`：`(user_id, user_type)` 联合唯一；`failed_login_count`、`locked`、`locked_until`、`lock_type`、`lock_reason_code` 等。
- `security_event`：登录成功/失败、锁定/解锁等事件经 recording 写入；`event_category`（AUTH / ACCOUNT / CREDENTIAL）。
- legacy `account_security_event`：**已物理删除**（migration `013`）；历史 migration `009` 正文保留作档案。
- 用户表冗余：`sys_user.locked` / `member_user.locked` 与 lock_state 同步（经 `UserAccountPort.updateLockStatus`）。

Member 锁定态建表迁移：`databases/migrations/009_member_account_protection.sql`；回滚 `rollback_009.sql`；基线 `databases/ingot_member.sql`。canonical 事件表见 `012_security_event_recording_schema.sql`；旧事件表下线见 `013_drop_account_security_event.sql`。

## 4. 登录事件回调链路

```
Auth 登录成功/失败
  → LoginEventListener（@Async）
    → userType=0 (ADMIN) → RemotePmsLoginRecordService → PMS InnerLoginRecordAPI
    → userType=1 (APP)   → RemoteMemberLoginRecordService → Member InnerLoginRecordAPI
  → RecordLoginUseCase.recordSuccess / recordFailure（userType 区分）
    → LockStatePort（incrementFailCount / resetFailCount）
    → SecurityEventPort（publishEvent）
    → 达阈值 → LockAccountUseCase.lockAutomatically
```

回调特性：

- **异步**：失败不阻断当次登录响应。
- **幂等安全**：缺 `userId` 时按 `username` 在对应用户体系查找；查不到静默忽略（防枚举）。
- **DTO 归属**：PMS / Member 各持 `LoginRecordDTO`（member-api 不依赖 pms-api）。

自动锁定在**本次失败异步回调落库后**生效；当次请求仍返回 bad credentials，下次登录才命中 locked（与 ADMIN 历史行为一致）。

锁定后继续 `recordFailure`：**仍发** `LOGIN_FAILURE`（电平遥测）；**跳过** `incrementFailCount` 与 `lockAutomatically`。

## 5. 解锁与失败计数清零

| 场景 | `failed_login_count` | 说明 |
|---|---|---|
| 登录成功 | 清零 | `RecordLoginUseCaseService.recordSuccess` |
| 手动解锁 | 清零 | `UnlockAccountUseCaseService.unlockManually` |
| 定时自动解锁 | 清零 | `UnlockAccountUseCaseService.doUnlockExpired`（`AccountLockTask` 每分钟扫描） |

## 6. 认证 meta（分级提示）

`AuthContextSupport` 在 LOGIN 阶段经 loader 读取 `maxAttempts` / `hintAfterAttempts`，结合 `LockStatePort` 的 `failedLoginCount` / `lockedUntil` 填充 meta，供 `DefaultUserCredentialChecker` 消费分级提示。

## 7. Member 持久化接入

`ingot-member-provider` 依赖 `ingot-security-account-adapter` 后：

- `LockStatePort` / `SecurityEventPort` 为真实 MyBatis 实现（非 NoOp）。
- `AccountLockTask` 自动注册，扫描 `ingot_member.account_lock_state` 过期锁定。
- 手动锁定/解锁（`BizUserServiceImpl`）经既有 `LockAccountUseCase` / `UnlockAccountUseCase` 落库。

`lockout.enabled=false` 时：不递增失败计数、不自动锁定、不发安全事件（与 ADMIN baseline 一致）。

## 8. 状态变更边沿事件

账号域 **A 类** DURABLE 事件仅在状态变化时 `publishEvent`（已是目标状态则 no-op）：

| 用例 | 边沿 |
|---|---|
| `lockManually` / `lockAutomatically` | 已 `locked=true` → 不发 `ACCOUNT_LOCKED`，不延长 `lockedUntil` |
| `unlockManually` / 过期自动解锁 | 已 `locked=false` → 不发 `ACCOUNT_UNLOCKED` |
| `enableAccount` / `disableAccount` | 目标状态与当前一致 → 不发 `ACCOUNT_ENABLED` / `ACCOUNT_DISABLED` |

`PASSWORD_*`、创建/删除等 **B 类** 仍靠用例单次动作，无额外短路。活动遥测（`LOGIN_FAILURE` / `LOGIN_SUCCESS`）为电平，不去重。

## 9. Redis 锁定信号与分层拦截

锁定 **transition 成功后** afterCommit 双写 Redis（解锁双删），fail-open：

```text
in:sec:account:locked:uid:{userType}:{userId}
in:sec:account:locked:name:{userType}:{username}
```

临时锁 TTL 至 `lockedUntil`；永久锁 TTL 默认 30 天（`ingot.security.account.signal.permanent-lock-ttl-days`，配在 `in-security-policy.yml`）。与网关 `in:gw:bl:tmp:*` 命名空间独立。

| 层级 | 职责 | 身份来源 |
|---|---|---|
| BFF | 加密登录短路（`ingot.security.account.bff`，默认 enabled，配在 `in-service-bff.yml`） | 解密后 username → name key；默认不调 Auth |
| Gateway | 已认证 API 短路（`ingot.security.account.gateway`，`AccountLockFilter`，配在 `in-service-gateway.yml`） | JWT `i` + OnlineToken/`ut` 补全的 userType → uid key；命中 **403** `ACCOUNT_LOCKED` |
| Auth | UserDetails 缓存兜底 | name key hit 则跳过 Feign，返回 `locked=true` |
| PMS/Member 领域层 | 边沿检测、事件、信号写入 | DB `LockStatePort` |

Gateway 瘦身 JWT 通常不含 `ut`：`AuthContextRelayFilter` 用 `jti` 读 `token:jti:{jti}`（OnlineToken.userType）补全；Auth 与 Gateway 须共用同一 Redis。无 JWT / 解析失败 / Redis down / exclude 路径 → **fail-open**。Gateway **不**解析 `/bff/**` body。

`AccountLockSignalPort` 在 account-core 装配（有 `StringRedisTemplate` 用 Redis，否则 NoOp）。账号用例（`UnlockAccountUseCaseService` 等）仅在 classpath 存在 account-adapter 时扫描，避免 BFF/Auth 误装事务依赖。

## 10. 已知限制 / 后续跟踪

1. **`attemptWindowMinutes` 滑动窗口**：已在 L4（[access-protection](../access-protection/README.md)）于 `RecordLoginUseCaseService` 实现；窗口外失败计数归零后再递增。
2. **V1 单元测试**：`InnerLoginRecordAPI`、`LoginEventListener` 自动化测试待后续补齐（验收以手工集成/regression 为准）。`LocalAccountLockoutPolicyLoader` 已有单元测试。
3. Gateway 明文 Auth token 路径的 name key 检查未做（生产主路径为 BFF 加密登录 + JWT uid Filter）。
4. 手动重复锁定不延长 `lockedUntil`；延长需求另开 change。
5. 安全中心前端页面不在本能力代码仓；管理面契约见 [PLATFORM-API.md](../../../changes/archive/2026/20260825-security-account-lockout-remote/PLATFORM-API.md)。
6. 生产 Nacos 默认仍为 `mode=local`；切 `remote` 需先执行 migration 018/019。
