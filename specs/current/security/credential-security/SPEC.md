# 凭证安全 SPEC

> 记录当前已验收并在线生效的系统事实。

## 1. 策略配置（`ingot.security.credential`）

统一开关：`ingot.security.credential.policy.mode = local | remote`。`local` → Nacos 本地配置；`remote` → 安全中心策略。

| 配置分组 | 关键属性 | 说明 |
|---|---|---|
| `policy.strength.*` | `minLength` / `requireUppercase` / `requireLowercase` / `requireDigit` / `requireSpecialChar` / `forbidUserAttributes` / `forbiddenPatterns` | 密码强度规则 |
| `policy.history.*` | `enabled` / `checkCount` | 历史密码去重，改密时校验最近 N 条 |
| `policy.expiration.*` | `enabled` / `maxDays` / `warningDaysBefore` / `graceLoginCount` | 密码过期与宽限登录次数 |
| `policy.initial-password.*` | `generation`(RANDOM/FIXED) / `length` / `fixedPassword` / `validHours` / `oneTime` / `forceChangeOnFirstLogin` | 初始密码生成与有效期 |

初始密码默认值（兼容现状）：`generation=RANDOM`、`length=10`、`fixedPassword=Ingot@123456`、`validHours=72`、`oneTime=true`、`forceChangeOnFirstLogin=true`。

## 2. 数据模型

- `password_expiration`（`ingot_core`）：`user_id`、`last_changed_at`、`expires_at`、`force_change TINYINT(1)`、`grace_login_remaining INT`、`next_warning_at`。
  - `force_change` 由迁移 `databases/migrations/008_add_force_change_password_expiration.sql` 补齐（存量按 `sys_user.must_change_pwd` 回填），回滚见 `rollback_008.sql`。
- `sys_user` / `member_user`：`must_change_pwd`、`password_changed_at`（复用，未新增列）。
- `expires_at` 为空时视为「永不过期」（`maxDays<=0`），`next_warning_at` 亦为空，判定不抛 NPE。

## 3. 宽限期扣减（登录成功链路）

```
登录 → AuthContextSupport(LOGIN 只读判定)
  - 硬过期(宽限耗尽) → credentialsNonExpired=false → CredentialsExpiredException
  - 软过期(宽限内)   → 放行
→ 认证成功 → RecordLoginUseCaseService.recordSuccess()
→ CredentialSecurityService.consumeGraceLoginOnSuccess(userId)
```

`consumeGraceLoginOnSuccess` 判定顺序（任一不满足返回 `-1`，不扣减）：

1. `userId != null`；
2. 已加载 `PasswordExpirationPolicy`（过期策略启用）；
3. `passwordExpirationService.isExpired(userId) == true`（确处于过期 / 宽限）。

满足时执行 `decrementGraceLogin`，剩余次数 `Math.max(0, n-1)`（幂等）。扣减异常仅告警，不阻断登录。

## 4. force_change / mustChangePwd 对齐

| 场景 | `must_change_pwd`(账号域) | `force_change`(凭证域) |
|---|---|---|
| 注册 / 管理员创建（含初始密码，且要求首登改密） | `true` | `1`（`markForceChange(true)`） |
| 管理员重置密码（`resetPassword`） | `true` | `1` |
| 用户改密成功 | `false` | `0`（`updateLastChanged` 内清除） |

`updateForceChange` 在记录不存在时静默跳过，不新建记录。

## 5. 初始密码（`DefaultInitialPasswordService`）

- `generate()`：`RANDOM` 生成长度 = `length`（保证含大写与数字，下限 6）；`FIXED` 返回 `fixedPassword`。ADMIN / Member 创建用户均改用此入口（替换原 `RandomUtil` / `randomPwd()`）。
- `isExpired(issuedAt)`：`validHours<=0` 或 `issuedAt` 为空 → `false`；否则 `now > issuedAt + validHours` → `true`。
- `isForceChangeOnFirstLogin()`：读取 `forceChangeOnFirstLogin`。

## 6. 强制改密访问限制（受限 scope）

强制改密期间**不使用独立拦截器**，而是登录签发受限 scope：

- `mustChangePwd=true` 时，`IdentityUtil`（PMS / Member）仅下发 `PermissionConstants.INIT_PASSWORD`（`in:init_pwd`）。
- 改密接口以 `@AdminOrHasAnyAuthority({INIT_PASSWORD})` 保护；资源服务基于 scope 鉴权天然拒绝其余接口。
- 改密成功后重新登录，恢复正常权限 scope。

## 7. Nacos 降级与动态刷新（`local` 模式）

- 凭证策略字段全部可经 Nacos 降级（strength / history / expiration / initial-password）。
- dataId：`in-security-policy.yml`（常量 `NacosConstants.IN_SECURITY_POLICY`）。
- 刷新机制：`CredentialSecurityProperties` 为 `@ConfigurationProperties` 随刷新更新；`LocalCredentialPolicyLoader` 监听 `NacosConfigRefreshEvent`，仅当 `dataId == in-security-policy.yml` 时 `evictAll()` 编译缓存，下次 `loadPolicies()` 按最新值重编译。不使用 `@RefreshScope`。
- 「用后失效」依赖 DB 执行状态，属执行数据而非策略配置，不随 Nacos 刷新。

## 8. 已知限制 / 后续跟踪

1. 初始密码 `validHours` 登录期硬超期拦截尚未接入（`isExpired` 已具备，缺登录期读取 `passwordChangedAt` 的调用点）。
2. Member 完整凭证持久化未落地：Member provider 未依赖 `ingot-security-credential-data`，`ingot_member` 未建 `password_history` / `password_expiration`；当前 Member 为域级对齐 + NoOp 持久化。
