# 凭证安全（密码引擎与登录/改密闭环）

> 能力域：`security` / `credential-security`

## 摘要

框架层密码引擎（强度 / 历史 / 过期）与账号域用例（注册 / 创建 / 改密 / 重置 / 登录）已收口为完整闭环，覆盖 ADMIN 与 Member 全部用户类型：

- 登录成功后，处于宽限期的软过期账号消费一次宽限次数；宽限耗尽的硬过期账号在认证阶段被拒。
- 初始密码由统一策略生成（随机 / 固定），支持有效期与首登强制改密。
- `password_expiration.force_change` 与账号域 `must_change_pwd` 语义对齐：创建 / 重置置位，改密成功清除。
- 强制改密期间通过签发**受限 scope**（`in:init_pwd`）约束仅可访问改密相关接口，无需独立拦截器。
- 初始密码策略统一经 `CredentialPolicyLoader` 取值，与强度/历史/过期共享同一降级阶梯；`ADMIN_CREATE` 的强制改密由 `forceChangeOnFirstLogin` 驱动，登录期 `validHours` 硬超期拦截落在账号域单点。
- `remote` 模式采用企业级弹性降级阶梯 `L1 Caffeine → L2 Redis → Resilient(remote → LKG → Nacos 地板)`，区分「远程失败」与「合法空」，**永不 fail-open**；当前生效来源经 actuator 端点 `credentialpolicy` 可观测。
- `local` 降级模式下，凭证策略经 Nacos 动态刷新（rebinder 重绑定 + 加载器即时编译），无需重启。

## 边界

- 本能力仅覆盖凭证（密码）安全：强度、历史、过期宽限、初始密码、强制改密对齐。
- **不含**：弱密码 / 泄露库识别（凭证风险）、MFA、账号登录失败锁定（属账号保护闭环）、统一安全事件中心。
- Member 侧为**域级对齐**：走账号域用例 + `AuthContextSupport` + 受限 scope；`PasswordExpirationService` / `PasswordHistoryService` 在 Member provider 为 NoOp，**尚无**独立凭证持久化（`ingot_member` 未建 `password_history` / `password_expiration`）。
- 弹性降级仅覆盖策略**配置读取**（含初始密码配置）；不含运行时服务发现级探活切换、安全中心策略管理台，以及远程调用熔断（P2 可选，本期未接入）。

## 所有者

- 策略引擎与服务：`ingot-security-credential`
- 通用持久化：`ingot-security-credential-data`（`sys_user` / `ingot_core` 侧）
- 账号域用例：`ingot-account-domain/ingot-account-core`
- 接入方：`ingot-pms-provider`、`ingot-member-provider`、`ingot-auth`

## 关联模块

| 职责 | 路径 |
|---|---|
| 凭证服务统一入口（校验 / 历史 / 过期 / 强制改密 / 宽限扣减） | `ingot-security-credential/.../DefaultCredentialSecurityService.java` |
| 初始密码生成与有效期判定 | `ingot-security-credential/.../DefaultInitialPasswordService.java` |
| 密码过期持久化（`force_change` / 宽限次数） | `ingot-security-credential-data/.../PasswordExpirationServiceImpl.java` |
| 弹性降级阶梯（remote → LKG → Nacos 地板） | `ingot-security-credential/.../internal/ResilientCredentialPolicyConfigService.java` |
| LKG 最近成功快照（Redis 唯一源） | `ingot-security-credential/.../internal/LastKnownGoodStore.java` |
| Nacos 地板（属性→VO，安全基线非空） | `ingot-security-credential/.../internal/LocalFloorSupplier.java` |
| 降级来源可观测（holder + actuator） | `ingot-security-credential/.../internal/CredentialPolicySourceHolder.java`、`.../actuate/CredentialPolicyEndpoint.java` |
| 本地策略即时编译（Nacos rebinder 刷新） | `ingot-security-credential/.../LocalCredentialPolicyLoader.java` |
| 注册 / 创建初始密码 + 强制改密置位 | `ingot-account-core/.../RegisterUserUseCaseService.java` |
| 改密清除 / 重置置位 强制改密 | `ingot-account-core/.../ChangePasswordUseCaseService.java` |
| 登录成功宽限扣减 | `ingot-account-core/.../RecordLoginUseCaseService.java` |
| 强制改密受限 scope 下发 | `ingot-pms-provider/.../identity/IdentityUtil.java`、`ingot-member-provider/.../identity/IdentityUtil.java` |

## 文档索引

- [SPEC](./SPEC.md)：策略配置、数据模型、宽限 / 强制改密 / 初始密码数据流、来源与弹性降级、可观测、Nacos 刷新
- 模块说明：`ingot-framework/ingot-security/ingot-security-credential/README.md`
- 来源变更：
  - `specs/changes/archive/2026/20260717-security-credential-closure/`（L1 收口）
  - `specs/changes/archive/2026/20260717-security-credential-resilience/`（弹性降级兜底与初始密码收口对齐）
