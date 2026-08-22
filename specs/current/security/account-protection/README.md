# 账号保护（登录失败锁定与安全事件）

> 能力域：`security` / `account-protection`

## 摘要

账号域登录失败计数、自动锁定、安全事件持久化与定时自动解锁已覆盖 **ADMIN（PMS）与 Member（APP）** 全部用户类型，共用同一套用例与表结构语义：

- 登录失败经 Auth 异步回调到对应服务（ADMIN→PMS、APP→Member），原子递增 `failed_login_count`，达阈值触发自动锁定。
- 锁定状态与失败计数**永远落 DB**（`account_lock_state`）；安全事件经 `CompositeSecurityEventPort` → recording 写入 canonical `security_event`（legacy `account_security_event` 表已物理下线）。
- 临时锁定到期后 `AccountLockTask` 自动解锁并清零失败计数；登录成功同样清零。
- 认证 meta 经 `AuthContextSupport` 填充失败次数、锁定截止时间与阈值提示。
- 状态变更 DURABLE 事件 **边沿触发**；锁定后 skip 失败计数递增，仍可发 `LOGIN_FAILURE`。
- 锁定态经 Redis 双 Key 同步；BFF 拦截加密登录、Gateway 拦截已认证 API（JWT + OnlineToken 补全 userType）、Auth 缓存兜底。
- lockout **策略参数**统一前缀 `ingot.security.account.*`，与 `ingot.security.credential.*` 命名对齐；消费侧一律经 `AccountLockoutPolicyLoader` seam 读取。
- 策略加载当前仅实现 `mode=local`（Nacos + rebinder 热刷新）；`mode=remote` 占位回退 local + WARN，remote 弹性阶梯由后续 change 接入（与事件 `ingot.security.event.target` 无关）。

## 边界

- 本能力覆盖**账号维度**登录失败锁定（`user_id + user_type`）。
- **不含**：remote 弹性阶梯（LKG / Nacos 地板 / L1-L2 缓存 / Actuator）、安全中心 account lockout 策略表与管理面。
- IP / 设备 / Client / 账号+IP 多维度防爆破见 [access-protection](../access-protection/README.md)（L4）。
- 跨服务事件聚合与中心入库见 [security-event-center](../security-event-center/README.md)；recording 框架见 [security-event-recording](../../framework/security-event-recording/README.md)。账号域发布使用 api `SecurityEventType`（仓库内唯一枚举）；code 字面量见 recording SPEC「事件 code SoT」。
- B端/C端差异**仅通过服务级 Nacos 配置**表达：PMS 严格（5/30/3），Member 短锁（5/15/3，禁止永久自动锁 `lockDurationMinutes=0`）。
- `lockout.enabled=false` 时保持 baseline：不写 lock_state / 不发安全事件、不自动锁定。

## 所有者

- 账号域用例与 seam：`ingot-framework/ingot-security/ingot-security-account/ingot-security-account-core`
- 持久化 adapter：`ingot-framework/ingot-security/ingot-security-account/ingot-security-account-adapter`
- 认证 meta：`ingot-framework/ingot-security/ingot-security-account/ingot-security-account-web-support`
- 接入方：`ingot-pms-provider`、`ingot-member-provider`、`ingot-auth`、`ingot-bff`、`ingot-gateway`

## 关联模块

| 职责 | 路径 |
|---|---|
| lockout 策略配置 | `ingot-security-account-core/.../config/AccountDomainProperties.java` |
| 策略加载 seam | `ingot-security-account-core/.../service/AccountLockoutPolicyLoader.java` |
| local 策略加载（即时映射 + 热刷新） | `ingot-security-account-core/.../service/impl/LocalAccountLockoutPolicyLoader.java` |
| 登录成功/失败记录与自动锁定 | `ingot-security-account-core/.../service/RecordLoginUseCaseService.java` |
| Redis 锁定信号 | `AccountLockSignalPort` / `RedisAccountLockSignalAdapter`；Key 见 `RedisKeyConstants.AccountLock` |
| BFF 登录短路 | `ingot-bff/.../BffAuthService.java` |
| Gateway 已认证拦截 | `ingot-gateway/.../AccountLockFilter.java`；userType 经 `AuthContextRelayFilter` + OnlineToken |
| Auth UserDetails 缓存 | `ingot-auth/.../CachingRemoteUserDetailsService.java` |
| 手动/自动解锁 | `ingot-security-account-core/.../service/UnlockAccountUseCaseService.java` |
| 锁定状态持久化 | `DefaultLockStatePortAdapter.java` → `account_lock_state` |
| 安全事件发布 | `CompositeSecurityEventPort.java` → `SecurityEventPublisher` → canonical `security_event` |
| 定时解锁任务 | `ingot-security-account-adapter/.../task/AccountLockTask.java` |
| 认证 meta 填充 | `ingot-security-account-web-support/.../AuthContextSupport.java` |
| Auth 登录事件分发 | `ingot-auth/.../event/LoginEventListener.java` |
| PMS 登录记录回调 | `ingot-pms-provider/.../web/inner/InnerLoginRecordAPI.java` |
| Member 登录记录回调 | `ingot-member-provider/.../web/inner/InnerLoginRecordAPI.java` |

## 文档索引

- [SPEC](./SPEC.md)：配置、数据模型、回调链路、B/C 差异化、local 热刷新、已知限制
- 来源变更：
  - `specs/changes/archive/2026/20260724-security-account-protection/`（L2 闭环 + remote 土台）
  - `specs/changes/archive/2026/20260806-security-event-legacy-cleanup/`（事件改写 canonical，停写 legacy 表）
  - `specs/changes/archive/2026/20260811-security-drop-account-security-event/`（legacy 表物理 DROP）
  - `specs/changes/archive/2026/20260811-security-event-edge-dedup-lock-shortcut/`（边沿事件 + Redis 锁定信号 + BFF/Gateway 短路）
  - `specs/changes/archive/2026/20260812-security-event-type-sot-cleanup/`（删除本地 `SecurityEventType`，改用 api 枚举）
