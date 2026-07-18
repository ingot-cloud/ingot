# 凭证安全（密码引擎与登录/改密闭环）

> 能力域：`security` / `credential-security`

## 摘要

框架层密码引擎（强度 / 历史 / 过期）与账号域用例（注册 / 创建 / 改密 / 重置 / 登录）已收口为完整闭环，覆盖 ADMIN 与 Member 全部用户类型：

- 登录成功后，处于宽限期的软过期账号消费一次宽限次数；宽限耗尽的硬过期账号在认证阶段被拒。
- 初始密码由统一策略生成（随机 / 固定），支持有效期与首登强制改密。
- `password_expiration.force_change` 与账号域 `must_change_pwd` 语义对齐：创建 / 重置置位，改密成功清除。
- 强制改密期间通过签发**受限 scope**（`in:init_pwd`）约束仅可访问改密相关接口，无需独立拦截器。
- `local` 降级模式下，凭证策略经 Nacos 动态刷新，无需重启。

## 边界

- 本能力仅覆盖凭证（密码）安全：强度、历史、过期宽限、初始密码、强制改密对齐。
- **不含**：弱密码 / 泄露库识别（凭证风险）、MFA、账号登录失败锁定（属账号保护闭环）、统一安全事件中心。
- Member 侧为**域级对齐**：走账号域用例 + `AuthContextSupport` + 受限 scope；`PasswordExpirationService` / `PasswordHistoryService` 在 Member provider 为 NoOp，**尚无**独立凭证持久化（`ingot_member` 未建 `password_history` / `password_expiration`）。
- 初始密码 `validHours` 仅约束「初始密码是否失效」的判定入口；登录期硬超期拦截尚未接入（见后续跟踪）。

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
| 本地策略编译与 Nacos 刷新驱逐 | `ingot-security-credential/.../LocalCredentialPolicyLoader.java` |
| 注册 / 创建初始密码 + 强制改密置位 | `ingot-account-core/.../RegisterUserUseCaseService.java` |
| 改密清除 / 重置置位 强制改密 | `ingot-account-core/.../ChangePasswordUseCaseService.java` |
| 登录成功宽限扣减 | `ingot-account-core/.../RecordLoginUseCaseService.java` |
| 强制改密受限 scope 下发 | `ingot-pms-provider/.../identity/IdentityUtil.java`、`ingot-member-provider/.../identity/IdentityUtil.java` |

## 文档索引

- [SPEC](./SPEC.md)：策略配置、数据模型、宽限 / 强制改密 / 初始密码数据流、Nacos 降级
- 模块说明：`ingot-framework/ingot-security/ingot-security-credential/README.md`
- 来源变更：`specs/changes/archive/2026/20260717-security-credential-closure/`
