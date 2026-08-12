# 账号保护 SPEC

> 记录当前已验收并在线生效的系统事实。

## 1. 策略配置（`ingot.security.account`）

统一开关：`ingot.security.account.mode = local | remote`（默认 `local`）。本期仅 `local` 有实现；配置为 `remote` 且无远程 Bean 时回退 `local` 并 WARN。

| 配置分组 | 关键属性 | 说明 |
|---|---|---|
| `lockout.*` | `enabled` / `maxAttempts` / `lockDurationMinutes` / `attemptWindowMinutes` / `hintAfterAttempts` | 登录失败锁定策略 |

默认值（代码 `@ConfigurationProperties`）：`enabled=true`、`maxAttempts=5`、`lockDurationMinutes=30`、`attemptWindowMinutes=15`、`hintAfterAttempts=3`。

**服务级差异化（Nacos）**：

| 服务 | dataId | 推荐值 | 说明 |
|---|---|---|---|
| PMS（B端） | `in-service-pms.yml` | 5 / 30 / 3 | 可配置永久锁（`lockDurationMinutes=0`） |
| Member（C端） | `in-service-member.yml` | 5 / 15 / 3 | **禁止** `lockDurationMinutes=0`（规避恶意锁号 DoS） |

旧前缀 `ingot.account.*` 已废弃，代码不再读取。

## 2. 策略加载 seam

消费侧（`RecordLoginUseCaseService`、`AuthContextSupport`）**只经** `AccountLockoutPolicyLoader.getLockoutPolicy()` 取不可变 `LockoutPolicy` 值对象，不直读 `@ConfigurationProperties`。

- `LocalAccountLockoutPolicyLoader`：每次调用即时从 `AccountDomainProperties` 映射，无进程内缓存。
- Nacos 变更经 `ConfigurationPropertiesRebinder` 重绑定后，下次读取即生效（免冷启动刷新）。
- 将来 `remote` 实现只需新增更高优先级 `AccountLockoutPolicyLoader` Bean，消费侧零改动。

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

## 8. 已知限制 / 后续跟踪

1. **remote 弹性与中心化**（L2 后续 change）：`RemoteAccountLockoutPolicyLoader`、Resilient/LKG/Nacos 地板/L1-L2/Invalidation/Actuator、安全中心 `account_lockout_policy_config` 表与管理 CRUD。依托现有 `mode` 与 seam，消费侧无需再改。
2. **`attemptWindowMinutes` 滑动窗口**：已在 L4（[access-protection](../access-protection/README.md)）于 `RecordLoginUseCaseService` 实现；窗口外失败计数归零后再递增。
3. **V1 单元测试**：`InnerLoginRecordAPI`、`LoginEventListener`、`LocalAccountLockoutPolicyLoader` 自动化测试待后续补齐（验收以手工集成/regression 为准）。
