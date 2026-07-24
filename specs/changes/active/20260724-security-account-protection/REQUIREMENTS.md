# Requirements

## 用户场景

### S1 Member 登录失败达阈值自动锁定

- 使用者：任意 Member（C端）用户。
- 触发条件：连续登录失败次数达到 `ingot.security.account.lockout.maxAttempts`（默认 5）。
- 期望结果：
  - 账号被自动锁定（`lock_type=AUTO`，`lock_reason_code=LOGIN_FAIL_EXCEED`），临时锁定按 `lockDurationMinutes` 计算 `locked_until`。
  - 锁定期内再次登录被拒绝，并提示剩余解锁时间（`locked_until`）。
  - 失败、锁定事件写入 `ingot_member.account_security_event`。

### S2 Member 登录成功清零失败计数

- 使用者：任意 Member 用户。
- 触发条件：未锁定状态下登录成功。
- 期望结果：`account_lock_state.failed_login_count` 清零，`last_login_at` / `last_login_ip` 更新，写入 `LOGIN_SUCCESS` 安全事件。

### S3 Member 锁定到期自动解锁

- 使用者：被临时锁定的 Member 用户。
- 触发条件：`locked_until <= now`，定时任务 `AccountLockTask`（每分钟）扫描。
- 期望结果：账号自动解锁，`failed_login_count` 归零，写入 `ACCOUNT_UNLOCKED` 安全事件，用户可重新登录。

### S4 Member 登录前分级提示

- 使用者：接近锁定阈值的 Member 用户。
- 触发条件：失败次数达到 `hintAfterAttempts`（默认 3）但未锁定。
- 期望结果：登录失败响应携带「已失败 N 次，达到 M 次将锁定」的分级提示（经 `AuthContextSupport` 填充 meta，`DefaultUserCredentialChecker` 消费）。

### S5 配置以 `ingot.security.account.*` 统一生效

- 使用者：运维 / 安全管理员。
- 触发条件：账号保护配置从 `ingot.account.*` 迁移为 `ingot.security.account.*`。
- 期望结果：所有账号 lockout 配置以新前缀读取；旧前缀 `ingot.account.*` 不再被任何代码读取；与 `ingot.security.credential.*` 命名对齐。

### S6 `mode=local` 免冷启动动态刷新

- 使用者：运维 / 安全管理员。
- 触发条件：`ingot.security.account.mode=local`（默认），在 Nacos 修改锁定阈值 / 时长。
- 期望结果：**无需重启服务**，下次登录失败判定即按新阈值 / 时长执行（经 `ConfigurationPropertiesRebinder` 重绑定属性，loader 每次即时读取）。

### S7 消费侧经 seam 取策略（为将来 remote 预留）

- 使用者：账号保护策略消费代码（`RecordLoginUseCaseService` / `AuthContextSupport`）。
- 触发条件：需要读取生效的 lockout 策略。
- 期望结果：一律经 `AccountLockoutPolicyLoader` 取策略，不直读 `@ConfigurationProperties`；将来 `mode=remote` 接入弹性阶梯时，消费侧无需改动。

## 业务规则

1. Member 与 ADMIN 复用同一套账号域用例（`RecordLoginUseCaseService` / `LockAccountUseCaseService` / `UnlockAccountUseCaseService`）与同一套策略语义，仅 `userType` 不同（APP vs ADMIN）。（P0）
2. 失败计数仅按**账号维度**（`user_id + user_type` 联合唯一）统计；IP / 设备 / Client 维度不在本闭环。（P0）
3. Auth 登录事件按 `userType` 精确分发：ADMIN→PMS 回调、APP→Member 回调，二者互不影响。（P0）
4. 登录记录回调为**异步**且**幂等安全**：回调失败仅告警不阻断登录；缺 `userId` 时按用户名在对应用户体系查找，查不到静默忽略（防枚举）。（P0）
5. `account_lock_state` / `account_security_event` 表结构与 ADMIN（`ingot_core`）完全一致，仅落在 `ingot_member` 库；`user_type` 存 `1`（APP）。（P0）
6. **配置命名统一**：账号 lockout 配置前缀改为 `ingot.security.account`；`AccountDomainProperties` 前缀随之迁移；旧前缀不保留双写（一次性迁移）。（P0）
7. **消费侧只经 `AccountLockoutPolicyLoader` seam 取策略**，不直读 `@ConfigurationProperties`；`LocalAccountLockoutPolicyLoader` 每次即时映射属性，保证 `local` 热刷新可感知。（P0）
8. **策略 vs 用户数据分离**：本 change 引入的 `mode` / seam 仅覆盖 lockout **策略参数**；`account_lock_state` 锁定状态与失败计数永远落 DB，不进入将来的 remote/LKG/缓存链。（P0）
9. **B端/C端 差异化**：引擎/表/用例统一，差异仅通过服务级配置表达。C端（Member）**不采用永久自动锁定**（`lockDurationMinutes` 不得为 0），必须「临时锁定 + 自动解锁」；B端（PMS）可更严格。（P1）
10. 自动解锁（`unlockExpired`）与登录成功一致地清零 `failed_login_count`，避免无滑动窗口下解锁后一次失败即再次锁定。（P1）
11. Member baseline 未启用锁定策略（`lockout.enabled=false`）时保持原有行为：仅 `member_user.locked` 冗余字段生效，不写 lock_state / 事件表、不自动锁定。（P1）
12. 自动锁定同时更新 `member_user.locked` 冗余字段（经 `UserAccountPort.updateLockStatus`），保证 Auth 侧 `IdentityUtil.map` 读取一致。（P1）

## 边界与非目标

- 非目标（拆为后续新 change）：remote 弹性阶梯（Resilient/LKG/Nacos 地板/L1/L2/Invalidation/Actuator）、安全中心 account lockout 表 / Feign / 管理面。本 change 只引入 `mode` 开关与 seam 抽象作为土台，`mode=remote` 暂不提供实现。
- 非目标：多维度失败保护（3.1）、异常登录画像（3.3）、统一安全事件中心跨服务聚合（L3）。
- 边界：`attemptWindowMinutes` 滑动窗口重置为已知设计缺口，本闭环不修复，记为后续跟踪项。
- 边界：自动锁定在「本次失败异步回调落库后」生效，当次请求仍返回 bad credentials，下次登录才命中 locked（与 ADMIN 现状一致）。
- 边界：`mode=remote` 在本 change 未实现，配置为 `remote` 时的占位行为（回退 local + WARN / fail-fast）作为待审阅决策点在 DESIGN 敲定。
- 兼容：不改变 ADMIN 侧锁定判定逻辑与 `account_lock_state` / `account_security_event` 表结构；`ingot_core` 库不受影响。
- 兼容：Member `member_user` 表结构不新增列（`locked` / `last_login_at` / `last_login_ip` 已存在）。

## 验收标准

- [ ] Member 连续失败达 `maxAttempts` 后被自动锁定，`ingot_member.account_lock_state` 落库（`user_type=1`、`lock_type=AUTO`）
- [ ] 临时锁定期内 Member 登录被拒绝并提示剩余解锁时间；`locked_until` 到期后 `AccountLockTask` 自动解锁且 `failed_login_count` 归零
- [ ] Member 登录成功后 `failed_login_count` 清零，`last_login_at/ip` 更新
- [ ] Member 失败 / 锁定 / 解锁 / 成功事件写入 `ingot_member.account_security_event`
- [ ] Auth `LoginEventListener` 对 ADMIN 走 PMS、对 APP 走 Member，二者均生效
- [ ] Member 失败达 `hintAfterAttempts` 时返回分级提示
- [ ] 账号 lockout 配置以 `ingot.security.account.*` 生效；全代码库无 `ingot.account.*` 残留读取
- [ ] `AccountLockoutPolicyLoader` seam 就位，`RecordLoginUseCaseService` / `AuthContextSupport` 经 loader 取策略
- [ ] `mode=local` 时修改 Nacos 锁定阈值 / 时长，无需重启，新请求即生效
- [ ] PMS（5/30/3）与 Member（5/15/3，禁 `lockDurationMinutes=0`）差异化默认值生效
- [ ] `ingot.security.account.lockout.enabled=false` 时 Member 保持 baseline 行为（不自动锁定、不写表）
- [ ] ADMIN 现有登录 / 锁定 / 解锁链路回归无破坏；相关模块编译通过
- [ ] `009` migration 在 `ingot_member` 执行成功，`rollback_009` 可回退
