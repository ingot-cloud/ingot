# Tasks

## 决策任务（已敲定）

- [x] D1：`LoginRecordDTO` 归属 → **方案 B（member-api 独立定义）**。理由：`ingot-auth` 已依赖 `member_api` 与 `pms_api`，而 `member-api` 不依赖 `pms-api`；在 member-api 自定义 DTO 可避免 member-api→pms-api 结合，各 service-api 自持 DTO。
- [x] D2：migration 编号 → **`009`**（现有最新 `008`，无并行占用）。
- [x] D3：Member 注册时 `lockStatePort.initialize` → **无需额外代码**：引入 adapter 后真实 Port 自动生效，回归验证注册链路即可。
- [x] D4：Nacos 动态刷新方式 → **rebinder 自动重绑定 `@ConfigurationProperties`**，loader 每次即时读取；实施后 V3 验证免冷启动生效，无需额外刷新监听。
- [x] D5：`mode=remote` 占位行为 → **回退 `local` + WARN**（可用性优先；remote 实现由后续 change 提供）。
- [x] D6：`LockoutPolicy` 载体 → **新建独立不可变 model**（`com.ingot.framework.security.account.domain.model.LockoutPolicy`），与 Properties 解耦，便于后续 remote 复用。
- [x] D7：`AccountDomainProperties` → **仅改前缀保类名**（`prefix=ingot.security.account`），改动最小。
- [x] D8：`unlockExpired` 清零 `failed_login_count` → **已满足**：`UnlockAccountUseCaseService.doUnlockExpired` 与 `recordSuccess` 均已调用 `resetFailCount`，无需补齐。

## 实施任务

### 闭环（A）

- [x] T1：Member 引入 `ingot-security-account-adapter` 依赖
  - 依赖：无
  - 验收：`ingot-member-provider` 启动后 `LockStatePort` / `SecurityEventPort` 为真实 adapter 实现（非 NoOp），`AccountLockTask` bean 存在
  - 实现：`ingot-member-provider/build.gradle` 增 `implementation project(ingot.framework_security_account_adapter)`

- [x] T2：`ingot_member` 库 DDL 迁移与基线同步
  - 依赖：D2
  - 验收：`009` 在 `ingot_member` 建 `account_lock_state` / `account_security_event` 成功；`rollback_009` 可回退；`databases/ingot_member.sql` 基线含两表
  - 实现：新增 `databases/migrations/009_member_account_protection.sql`（`USE ingot_member` + 复用 adapter 内置两表 DDL）+ `databases/migrations/rollback_009.sql`；同步 `databases/ingot_member.sql`

- [x] T3：新增 Member 登录记录 Feign 接口
  - 依赖：D1
  - 验收：`RemoteMemberLoginRecordService.record` 可被 Auth 调用，contextId 唯一，指向 MEMBER_SERVICE
  - 实现：`ingot-member-api` 新增 `RemoteMemberLoginRecordService`（`/inner/user/login/record`）；按 D1 处理 `LoginRecordDTO`

- [x] T4：新增 Member `InnerLoginRecordAPI`
  - 依赖：T1、T3
  - 验收：成功 / 失败均正确调用 `RecordLoginUseCase`（`userType=APP`）；缺 userId 按用户名查 Member 用户；查不到静默忽略
  - 实现：`ingot-member-provider` 新增 `InnerLoginRecordAPI`（`@Permit(mode=INNER)`，`/inner/user/login/record`），镜像 PMS，`userType=UserTypeEnum.APP`

- [x] T5：改造 Auth `LoginEventListener` 按 userType 分发
  - 依赖：T3
  - 验收：ADMIN→PMS、APP→Member 分别回调；二者互不影响；非 ADMIN/APP 忽略
  - 实现：注入 `RemoteMemberLoginRecordService`，移除「非 ADMIN 直接 return」，`onLoginSuccess` / `onLoginFailure` 按 `payload.getUserType()` 分发

- [x] T6：Member 手动锁定 / 解锁落库对齐（回归确认）
  - 依赖：T1、T2
  - 验收：`BizUserServiceImpl.lockAccount/unlockAccount` 引入 adapter 后同步落 `account_lock_state` 与安全事件
  - 实现：确认现有调用链在真实 Port 下正确落库，必要时补齐

- [x] T7：`unlockExpired` / `recordSuccess` 清零失败计数确认（按 D8）
  - 依赖：D8、T1
  - 验收：自动解锁与登录成功后 `failed_login_count` 归零；解锁后一次失败不会立即再次锁定
  - 实现：确认 `UnlockAccountUseCaseService.unlockExpired` 行为，缺失则补齐（两端一致）

### 土台（B）

- [x] T8：`AccountDomainProperties` 改前缀 + 新增 `mode`（按 D7）
  - 依赖：无
  - 验收：前缀 `ingot.security.account`；`mode`（默认 `local`）可读；旧前缀不再绑定
  - 实现：改 `@ConfigurationProperties(prefix="ingot.security.account")`，增 `mode` 字段

- [x] T9：新增 `AccountLockoutPolicyLoader` seam + `LocalAccountLockoutPolicyLoader`（按 D6）
  - 依赖：T8
  - 验收：loader 返回生效 `LockoutPolicy`；`Local` 实现每次即时映射属性（热刷新可感知）
  - 实现：`ingot-security-account-core` 新增 `service/AccountLockoutPolicyLoader` + `impl/LocalAccountLockoutPolicyLoader`；`AccountDomainAutoConfiguration` 按 `mode` 装配（`remote` 占位按 D5）

- [x] T10：消费侧改经 loader 取策略
  - 依赖：T9
  - 验收：`RecordLoginUseCaseService` / `AuthContextSupport` 不再直读 `@ConfigurationProperties`，判定逻辑不变
  - 实现：两处注入 `AccountLockoutPolicyLoader`，替换 `accountProperties.getLockout()` 读取点

- [x] T11：Nacos 键迁移 + B/C 差异化值
  - 依赖：T8
  - 验收：PMS（5/30/3）、Member（5/15/3，禁 `lockDurationMinutes=0`）差异化生效；旧键无残留
  - 实现：`in-service-pms.yml`（DEV/TEST/PROD）改 `ingot.account.*`→`ingot.security.account.*`；`in-service-member.yml` 新增差异化 `ingot.security.account.lockout.*` + `mode: local`

## 验证任务

- [x] V1：单元测试 — 本期未补齐自动化单测，验收以手工集成/regression 为准（记入 current SPEC §8）
- [x] V2：集成测试（Member「失败→锁定→到期解锁→再登录」全链路；两表落库；ADMIN/APP 共存互不干扰）
- [x] V3：免冷启动刷新验证（`in-service-member.yml` 改 `maxAttempts` / `lockDurationMinutes` 不重启生效）
- [x] V4：回归（ADMIN 登录/锁定/解锁不受影响；Member baseline `enabled=false` 不自动锁定；无 `ingot.account.*` 残留读取；相关模块编译通过）
- [x] V5：迁移验证（`009` 执行成功、`rollback_009` 可回退）

## 完成检查

- [x] 实现与 DESIGN 一致
- [x] REQUIREMENTS 验收标准全部满足
- [x] Current 已更新（`specs/current/security/account-protection/`）
- [x] roadmap 状态表 L2 更新为 done
- [x] 后续 change（remote 弹性与中心化）已登记到 roadmap / 本 change 后续跟踪
- [x] Change 已记录完成信息并归档
