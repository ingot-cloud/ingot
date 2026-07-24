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

四类策略统一映射为 `CredentialPolicyType`（`STRENGTH` / `HISTORY` / `EXPIRATION` / `INITIAL_PASSWORD`），均可经安全中心下发或 Nacos 兜底（见 §7）。

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

初始密码配置**不再直连降级源**，统一经 `CredentialPolicyLoader.getInitialPasswordConfig()` 取生效值（`INITIAL_PASSWORD` 策略类型），因此与 strength/history/expiration 共享同一「远程优先 + 兜底」降级阶梯（见 §7）。

- `generate()`：`RANDOM` 生成长度 = `length`（保证含大写与数字，下限 6）；`FIXED` 返回 `fixedPassword`。ADMIN / Member 创建用户均改用此入口（替换原 `RandomUtil` / `randomPwd()`）。
- `isExpired(issuedAt)`：`validHours<=0` 或 `issuedAt` 为空 → `false`；否则 `now > issuedAt + validHours` → `true`。
- `isForceChangeOnFirstLogin()`：读取生效配置的 `forceChangeOnFirstLogin`。

接线：

- `RegisterUserUseCaseService` 的 `ADMIN_CREATE` 由 `isForceChangeOnFirstLogin()` 决定 `mustChangePwd`（命令显式值优先）；`SELF_REGISTER` 保持默认 `true`。
- 登录期初始密码 `validHours` 硬超期拦截落点为 `AuthContextSupport`（账号域单点）：`mustChangePwd` 且 `passwordChangedAt + validHours < now` 时置 `credentialsNonExpired=false` 阻断并引导重置；可选依赖（`InitialPasswordService` / `UserAccountPort`）缺失时降级跳过。

## 6. 强制改密访问限制（受限 scope）

强制改密期间**不使用独立拦截器**，而是登录签发受限 scope：

- `mustChangePwd=true` 时，`IdentityUtil`（PMS / Member）仅下发 `PermissionConstants.INIT_PASSWORD`（`in:init_pwd`）。
- 改密接口以 `@AdminOrHasAnyAuthority({INIT_PASSWORD})` 保护；资源服务基于 scope 鉴权天然拒绝其余接口。
- 改密成功后重新登录，恢复正常权限 scope。

## 7. 策略来源、缓存与弹性降级

统一入口 `CredentialPolicyLoader`（`loadPolicies()` + `getInitialPasswordConfig()`），两条加载路径**同源同节奏**：均经 `CredentialPolicyConfigService.getAll()` 取原始配置后即时编译，**不再有无 TTL 的进程内编译缓存**（`LocalCompiledPolicyCache` 已移除）。稳态新鲜度上界 = L1 TTL。

### 7.1 `remote` 模式降级阶梯

`getAll()` 由内到外的委托链：

```
L1 Caffeine(TTL) → L2 Redis(TTL) → ResilientCredentialPolicyConfigService
                                        remote(新鲜) → LKG(最近成功快照) → Nacos 地板
```

- 远端 delegate（`RemoteCredentialPolicyConfigService`）**区分失败与合法空**：调用异常 / 连接失败 / 非成功码统一抛 `CredentialRemoteUnavailableException`；成功（含空）返回真实数据。
- `ResilientCredentialPolicyConfigService`：远程成功 → 刷新 LKG 并返回（成功空 = 合法无策略，直接接受，不兜底）；远程失败 → 回退 LKG；LKG 缺失 → 回退 Nacos 地板；**永不 fail-open 到「无策略」**。
- 失败态返回的是**有效兜底值**（非失败空），L1/L2 本就不缓存空；远程恢复后随 L1/L2 短 TTL 过期自动回到新鲜值。
- `ingot-security-provider` 的本地 Mapper delegate **不包裹** Resilient（无远程失败语义）。

### 7.2 LKG（Last-Known-Good）

- 存储：Redis 独立命名空间 key `in:credential:policy:lkg`，长存 / 不过期，仅在**远程成功**时刷新（含成功空）。
- 与 L1/L2 热缓存**分离**：`InvalidationBus` / 失效事件**不清** LKG。
- **Redis 为唯一 LKG 源**（无进程内 `AtomicReference` 副本）：Redis 不可用 / 缺失 / 读异常时 `load()` 返回 `null` → 全集群统一落 Nacos 地板，避免负载均衡下热/冷节点在 LKG 与地板间分叉。

### 7.3 Nacos 地板（`LocalFloorSupplier`）

- 由本地属性（`ingot.security.credential.policy.*`）映射为 `List<CredentialPolicyConfigVO>`：strength/history/expiration 按启用纳入，初始密码始终纳入。
- 必须维护**安全基线**：全部校验类关闭时补最小强度基线，**永不返回空**（否则 D-B 合法空语义会退回 fail-open）。
- 默认开关 `ingot.security.credential.fallback.local-floor-enabled = true`（可用性优先）。

### 7.4 `local` 模式与 Nacos 动态刷新

- `mode=local`：仅走 Nacos 本地配置，**无远程调用**。凭证策略字段全部可降级（strength / history / expiration / initial-password）。
- dataId：`in-security-policy.yml`（常量 `NacosConstants.IN_SECURITY_POLICY`）。
- 刷新机制：`CredentialSecurityProperties` 为 `@ConfigurationProperties`，Nacos 变更经 `ConfigurationPropertiesRebinder` 重绑定；因加载器每次即时编译，下次 `loadPolicies()` / `getInitialPasswordConfig()` 即读到最新值。**不使用 `@RefreshScope`**，也不再监听 `NacosConfigRefreshEvent`。
- 「用后失效」依赖 DB 执行状态，属执行数据而非策略配置，不随 Nacos 刷新。

### 7.5 缓存 TTL 配置

- `CredentialCacheProperties.l1Ttl` / `l2Ttl` 为 `Duration`，标注 `@DurationUnit(ChronoUnit.MINUTES)`：无单位裸数值按**分钟**解析，避免被 Spring Boot 默认按毫秒绑定导致「写入即过期」的缓存假性未命中。

## 8. 可观测（降级来源）

- `CredentialPolicySourceHolder` 记录当前生效来源（`REMOTE` / `LAST_KNOWN_GOOD` / `LOCAL_FLOOR`）、LKG/地板降级计数与最近降级时间；走兜底时打 WARN 日志。
- Actuator 端点 `credentialpolicy` 暴露上述状态（`@ConditionalOnClass` 守护，缺 actuator 依赖时不注册）。

## 9. 已知限制 / 后续跟踪

1. Member 完整凭证持久化未落地：Member provider 未依赖 `ingot-security-credential-data`，`ingot_member` 未建 `password_history` / `password_expiration`；当前 Member 为域级对齐 + NoOp 持久化。
2. 安全中心 `INITIAL_PASSWORD` 本期仅**预留类型 + 远程可读**，可视化管理台后续 change。
3. 远程调用熔断（Sentinel）为 P2 可选增强，本期未接入；以「每次远程失败即兜底 + 不缓存失败」保证正确性。
