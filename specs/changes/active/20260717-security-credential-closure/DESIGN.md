# Design

## 方案摘要

复用现有框架密码引擎（`ingot-security-credential` + `ingot-security-credential-data`）与账号域用例（`ingot-account-domain`），不新建库表、不改动 `RemoteCredentialService` 契约。围绕四个缺口做「接入 + 对齐」，并统一 `local` 模式下的 Nacos 动态刷新。

分层职责保持不变：

| 层 | 组件 | 本次改动 |
|----|------|----------|
| 策略引擎 | `PasswordExpirationPolicy` / `PasswordValidator` | 复用，不改判定逻辑 |
| 凭证服务 | `CredentialSecurityService` / `DefaultCredentialSecurityService` | 新增登录成功后宽限扣减入口；对齐 `force_change` 读写 |
| 持久化 | `PasswordExpirationService(Impl)` / `PasswordExpiration` | 领域模型补 `force_change` 映射；`initExpiration` 修正 |
| 账号用例 | `RegisterUserUseCaseService` / `ChangePasswordUseCaseService` | 初始密码策略接入；改密同步清除 `force_change` |
| 认证上下文 | `AuthContextSupport` | 硬 / 软过期判定保持只读；不在此扣减 |
| 拦截 | 新增强制改密拦截器（网关或资源服务） | 强制改密状态限制访问范围 |
| 配置 | `CredentialSecurityProperties` + 新增初始密码配置 | Nacos 动态刷新 |

## 数据模型与接口

### 数据模型

- `password_expiration` 表：结构不变（`force_change TINYINT(1)`、`grace_login_remaining INT` 已存在）。
- 领域模型 `PasswordExpiration`：**补映射 `force_change` 字段**（当前缺失），使读写与 DB 一致。
- 账号表 `sys_user` / `member_user`：`must_change_pwd`、`password_changed_at` 已存在，不新增列。
- 初始密码策略配置：新增 `ingot.security.credential.policy.initial-password.*`（见下）。

### 接口

- `PasswordExpirationService`：`decrementGraceLogin(Long userId)` 已存在，本次接入调用方；补充按 `force_change` 读写所需方法（如 `markForceChange` / 在 `updateLastChanged` 时清除）。
- 新增初始密码能力接口（位置待定，倾向账号域）：`generateInitialPassword(policy)`、`isInitialPasswordExpired(userId)`。
- 不改动 `RemoteCredentialService.getPolicyConfigs()` 契约。

## 数据流与失败处理

### 宽限期扣减（缺口 1）

```
登录请求 → 加载 UserDetails（PMS/Member）
  → AuthContextSupport.fill(): LOGIN 场景 validate（只读判定 硬过期/软过期）
     - 硬过期(宽限耗尽) → credentialsNonExpired=false → Auth 抛 CredentialsExpiredException
     - 软过期(宽限内)   → 放行，标记「本次为宽限登录」
  → 认证成功 → 登录成功回调
  → 对软过期登录调用 decrementGraceLogin(userId)（幂等：Math.max(0, n-1)）
```

失败处理：扣减失败仅告警不阻断登录（与现有 `AuthContextSupport` 异常放行一致）。

### 初始密码（缺口 2）

```
管理员创建/重置
  → 按 initial-password 策略生成初始密码(RANDOM|FIXED)
  → mustChangePwd=true + force_change=1 + 记录初始密码签发时间
  → 首次登录：若初始密码超 validHours → 视为过期，拒绝并要求重置
  → 用户改密成功 → 清除 mustChangePwd/force_change，初始密码即失效(oneTime)
```

### force_change / mustChangePwd 对齐（缺口 3）

- 判定「必须改密」= `mustChangePwd == true || password_expiration.force_change == 1`。
- 改密成功（`changePassword` / `forceChangePassword`）→ 同时清 `mustChangePwd=false` 且 `force_change=0`。
- `resetPassword` / `ADMIN_CREATE` → 同时置 `mustChangePwd=true` 且 `force_change=1`。

### 强制改密访问限制（缺口 4）

```
已登录请求 → 判定 mustChangePwd/force_change
  → 命中且非白名单接口 → 拒绝(如 428 Precondition Required / 业务错误码) + 引导改密
  → 白名单(改密/登出/用户信息) → 放行
```

失败处理：拦截器读取用户态失败时放行（避免误伤），依赖登录判定兜底。

## Nacos 降级与动态刷新设计

> 遵循 [roadmap 横切原则](../../../../docs/requirements/themes/security-center-roadmap.md)。

- 统一开关：`ingot.security.credential.policy.mode = local | remote`。`local` → Nacos；`remote` → 安全中心快照。
- 可 Nacos 降级字段（`local` 模式）：
  - `ingot.security.credential.policy.strength.*`（强度全部字段）
  - `ingot.security.credential.policy.history.*`（`enabled` / `checkCount`）
  - `ingot.security.credential.policy.expiration.*`（`enabled` / `maxDays` / `warningDaysBefore` / `graceLoginCount`）
  - `ingot.security.credential.policy.initial-password.*`（本次新增：`generation` / `fixedPassword` / `validHours` / `oneTime` / `forceChangeOnFirstLogin`）
- 对应 Nacos dataId：沿用现有 `application-security.yml`（`refreshEnabled=true`，已在各服务 `spring.config.import` 引入）承载 `ingot.security.credential.*`。
- 动态刷新落地：
  1. `CredentialSecurityProperties` 为 `@ConfigurationProperties`，随 Nacos 刷新更新字段值。
  2. **验证策略对象是否随刷新重建**：`local` 策略加载器若在启动时固化策略实例，则刷新不生效。需确认 `LocalCredentialPolicyLoader` 每次 `loadPolicies()` 是否读取最新 properties；若缓存则加 `@RefreshScope` 或监听 `RefreshEvent` 重建。
  3. 不可降级项：无（凭证策略全部可降级）。初始密码「用后失效」依赖 DB 状态，属执行数据不属策略配置。
- 验证方式：`mode=local` 下改 Nacos 中 `expiration.maxDays` / `strength.minLength`，不重启，触发一次登录 / 改密，观察行为按新值执行。

## 迁移与回滚

- **DDL 变更（修正）**：实测线上基线 `databases/ingot_core.sql` 的 `password_expiration` **无 `force_change` 列**（仅 credential-data 内置 DDL 有）。已补：基线加列 + 迁移 `databases/migrations/008_add_force_change_password_expiration.sql`（含存量按 `sys_user.must_change_pwd` 回填）+ 回滚 `rollback_008.sql`。列默认 0，兼容存量。
- 初始密码策略默认值保持「兼容现状」：`generation=RANDOM`、`oneTime=true`、`forceChangeOnFirstLogin=true`、`validHours` 给较宽松默认（如 72）。默认不改变现有 `ADMIN_CREATE` 行为（已 `mustChangePwd=true`）。
- 回滚：各改动均为增量接入，回退代码即恢复原行为；无数据不可逆操作。

## 测试策略

- 单元：宽限扣减幂等与边界（remaining=0）、force_change/mustChangePwd 对齐、初始密码过期判定、初始密码生成。
- 集成：ADMIN 与 Member 各跑一遍「创建→首登强制改密→改密→再登录」全链路。
- 降级：`mode=local` 修改 Nacos 配置动态刷新验证。
- 回归：现有 PMS 登录 / 改密链路、`AuthContextSupport` 硬过期阻断不受影响。

## 实施偏差与决策（对应 AGENTS.md 规则 4）

1. **T5 强制改密拦截落点（D2 由网关改为方案 B，已确认）**：原定网关统一拦截，但网关 [AuthContextRelayFilter](../../../../ingot-service/ingot-gateway/src/main/java/com/ingot/cloud/gateway/filter/auth/AuthContextRelayFilter.java) 仅读 JWT `i` claim（不验签、不鉴权）、JWT 瘦身不含 `mustChangePwd`，不适合。改采**方案 B：登录时按 `mustChangePwd` 签发受限 scope**。
   - 结论：**已在现网实现**。[PMS IdentityUtil](../../../../ingot-service/ingot-pms/ingot-pms-provider/src/main/java/com/ingot/cloud/pms/identity/IdentityUtil.java) 与 [Member IdentityUtil](../../../../ingot-service/ingot-member/ingot-member-provider/src/main/java/com/ingot/cloud/member/identity/IdentityUtil.java) 在 `mustChangePwd=true` 时仅下发 `PermissionConstants.INIT_PASSWORD`（`in:init_pwd`）；改密接口以 `@AdminOrHasAnyAuthority({INIT_PASSWORD})` 保护。资源服务基于 scope 鉴权，天然拒绝其余接口，无需新增拦截。
   - 后续增强（非本闭环）：初始密码 `validHours` 硬超期在登录期拦截。

2. **T6 Member 完整持久化（拆为后续 change，已确认）**：Member provider 仅含 `ingot-security-credential`（`PasswordExpirationService`/`PasswordHistoryService` 为 NoOp）。本闭环仅完成 Member **域级对齐**（走账号域用例 + `AuthContextSupport` + 受限 scope）。完整持久化（增 `credential-data` 依赖 + `ingot_member` 建 `password_history`/`password_expiration` DDL）因涉及 DDL 与 Member baseline 定位，拆为后续独立 change。

## 待审阅决策点（实施前需在 TASKS 敲定）

1. **宽限扣减的调用位置**：
   - 方案 A（推荐）：账号域登录成功用例统一扣减，需为 Member 补最小登录成功回调（注意与 L2 边界，仅做凭证宽限，不做失败计数）。
   - 方案 B：Auth 侧 `AccessTokenAuthenticationSuccessHandler` 通过新增内网接口回调扣减。
   - 方案 C：`AuthContextSupport` 判定为软过期时即扣减（偏差：认证可能后续失败导致误扣）。
2. **强制改密拦截位置**：网关统一拦截 vs 各资源服务过滤器。倾向网关（集中、覆盖全应用）。
3. **初始密码能力归属**：账号域（`ingot-account-domain`）新增 vs 凭证模块新增。倾向账号域（贴近创建 / 重置用例）。
4. **Member 凭证装配确认**：核实 Member provider 是否已装配 `CredentialSecurityService` / `AuthContextSupport`；若未装配需补 Bean。
