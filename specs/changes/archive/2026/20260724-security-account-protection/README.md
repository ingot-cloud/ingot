# 账号保护全用户闭环 + remote 弹性架构土台（L2）

> 状态：completed（已验收，已更新 current，已归档）

## 元数据

| 项 | 值 |
|---|---|
| Change ID | `20260724-security-account-protection` |
| 领域 | `security` |
| 负责人 | jy |
| 创建日期 | 2026-07-24 |
| 目标发布日期 | TBD |

## 目标

本 change 有两层目标：

**目标一（闭环）**：把 ADMIN（PMS）侧已成熟的账号保护闭环——**登录失败计数 → 达阈值自动锁定 → 安全事件持久化 → 定时自动解锁 → 认证 meta 友好提示**——完整扩展到 Member（`UserTypeEnum.APP`）用户，消除 Member 侧「三层欠缺」：

1. **依赖层**：`ingot-member-provider` 未引入 `ingot-security-account-adapter`，`LockStatePort` / `SecurityEventPort` 回退到 NoOp（不写库、仅日志），`AccountLockTask` 定时解锁 bean 不存在。
2. **数据层**：`ingot_member` 库无 `account_lock_state` / `account_security_event` 表（`002_upgrade_member_user.sql` 明确交由模块依赖 SQL 管理）。
3. **接线层**：Auth 侧 `LoginEventListener` 对非 ADMIN 用户直接 `return`；Member 无 `InnerLoginRecordAPI` / `RemoteMemberLoginRecordService` 登录记录回调入口。

**目标二（土台）**：为账号保护策略对齐 L1 凭证的「remote/local 双模式 + LKG + Nacos 兜底 + 免冷启动刷新」架构**打好土台**。参照 L1 凭证「closure → resilience」两阶段交付节奏，本 change 只做土台，remote 弹性阶梯与安全中心中心化另立新 change：

- 配置命名统一 `ingot.account.*` → `ingot.security.account.*`（与 `ingot.security.credential.*` 对齐）。
- 引入 `ingot.security.account.mode`（`local` | `remote`，默认 `local`）开关，为将来 remote 预留。
- 引入 `AccountLockoutPolicyLoader` seam 抽象（本期仅 `Local` 实现），消费侧改经 loader 取策略，使将来接入 remote 无需再改消费侧。
- `local` 模式免冷启动刷新（改 Nacos 配置无需重启即生效）。

**核心原则**（承接凭证「策略 vs 用户数据」分离）：将来可 remote/LKG/缓存的仅是 **lockout 策略参数**（阈值 / 时长）；**`account_lock_state` 锁定状态与失败计数永远落 DB、不可降级**。

## 范围

**包含：**

- Member `ingot-security-account-adapter` 依赖接入，`LockStatePort` / `SecurityEventPort` 切换为真实持久化实现。
- `ingot_member` 库新增 `account_lock_state` / `account_security_event` 表（migration + 回滚 + 基线 SQL 同步）。
- Auth → Member 登录记录回调链路：新增 `RemoteMemberLoginRecordService`（Feign）+ Member `InnerLoginRecordAPI`（`userType=APP`）。
- 改造 Auth `LoginEventListener`：按 `userType` 分发 ADMIN→PMS、APP→Member，移除「非 ADMIN 直接 return」。
- **配置改名**：`ingot.account.*` → `ingot.security.account.*`（`AccountDomainProperties` 前缀 + PMS/Member Nacos 键一次性迁移）。
- **mode 开关**：新增 `ingot.security.account.mode`（默认 `local`）。
- **seam 抽象**：新增 `AccountLockoutPolicyLoader` + `LocalAccountLockoutPolicyLoader`；消费侧 `RecordLoginUseCaseService` / `AuthContextSupport` 改经 loader 取策略。
- **local 免冷启动刷新**：确认 `mode=local` 下 Nacos 配置变更无需重启即生效。
- **B端/C端 差异化默认值**：引擎/表/用例统一，仅配置差异化（PMS 严格、Member 短锁且禁永久自动锁）。
- Member 认证 meta 对齐：登录时经 `AuthContextSupport` 填充 `failedLoginCount` / `lockedUntil` / 阈值提示。

**不包含（拆为后续新 change）：**

- 账号 lockout **remote 弹性阶梯**：`RemoteAccountLockoutPolicyLoader`、`Resilient` 兜底、`LastKnownGoodStore`（LKG）、`LocalFloorSupplier`（Nacos 地板）、L1 Caffeine / L2 Redis 缓存、`InvalidationBus` 跨节点失效、Actuator 来源可观测。
- **安全中心中心化**：`ingot-security` 新增 account lockout 策略表（如 `account_lockout_policy_config`）+ `RemoteAccountLockoutService` Feign + 管理面 CRUD + 失效发布。
- 多维度失败保护（IP / 设备 / Client / 账号+IP，需求 3.1）——归 L4 访问防护。
- 异常登录识别（需求 3.3）、统一安全事件中心（需求九，L3）。
- `attemptWindowMinutes` 滑动窗口失败计数重置（现状配置项已定义但未生效）。
- ADMIN 现有锁定判定逻辑变更（本闭环只做对齐 + 土台，不改 PMS 既有行为）。

## 工件

- [需求](./REQUIREMENTS.md)
- [设计](./DESIGN.md)
- [任务](./TASKS.md)

## 完成记录

- 完成日期：代码完成 2026-07-24；验收通过 2026-07-29
- 关联提交或 PR：TBD
- 更新的 current capability：`specs/current/security/account-protection/`（README + SPEC，新建）
- 与原设计的差异：
  - 按 D1 采用 member-api 独立 `LoginRecordDTO`，未复用 pms-api DTO。
  - D8 确认 `unlockExpired` / `recordSuccess` 已有清零逻辑，无需额外补齐。
  - V1 单元测试未在本 change 补齐，验收以手工集成 / 回归 / migration 执行为准（记入 current SPEC 已知限制）。
- 取消原因：—

## 后续跟踪（拆出为新 change）

1. **账号 lockout remote 弹性与中心化**：镜像 `20260717-security-credential-resilience`，实现 `RemoteAccountLockoutPolicyLoader` + `Resilient/LKG/LocalFloor/L1/L2/Invalidation/Actuator` + 安全中心 `account_lockout_policy_config` 表 / `RemoteAccountLockoutService` Feign / 管理 CRUD。依托本 change 的 `mode` 开关与 `AccountLockoutPolicyLoader` seam，无需再改消费侧。
2. `attemptWindowMinutes` 滑动窗口失败计数重置。
