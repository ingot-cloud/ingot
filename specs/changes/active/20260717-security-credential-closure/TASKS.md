# Tasks

## 决策任务（实施前必须敲定，对应 DESIGN 待审阅决策点）

- [x] D1：宽限扣减调用位置 → **方案 A（账号域登录成功用例统一扣减）**
- [x] D2：强制改密拦截位置 → **网关统一拦截**
- [x] D3：初始密码能力归属 → **账号域 `ingot-account-domain`**
- [x] D4：Member 凭证装配现状确认结论 → 实施首步核实（T0）

## 实施任务

- [x] T1：`PasswordExpiration` 领域模型补 `force_change` 字段映射，`initExpiration` 修正（`nextWarningAt` / `force_change` 初始化）
  - 依赖：无
  - 验收：读写 `force_change` 与 DB 一致，`initExpiration` 不再遗漏字段
  - 实现：`PasswordExpiration` 增 `forceChange`；`PasswordExpirationServiceImpl.initExpiration/updateLastChanged` 补 `forceChange`/`nextWarningAt`（并修复 maxDays=0 时 NPE）；`updateForceChange` 新增；`NoOpPasswordExpirationService` 同步

- [x] T2：force_change / mustChangePwd 语义对齐
  - 依赖：T1
  - 验收：改密成功同时清零两者；重置 / 管理员创建同时置位两者；登录判定读取二者并集
  - 实现：`CredentialSecurityService.markForceChange`；`RegisterUserUseCaseService`（ADMIN_CREATE 且 mustChangePwd 置位）、`ChangePasswordUseCaseService.resetPassword`（置位）、`changePassword/forceChangePassword` 经 `updateLastChanged` 清零。登录判定读取 `mustChangePwd`（force_change 与之保持同步，等价并集）

- [x] T3：宽限期扣减接入登录成功链路（按 D1）
  - 依赖：T1、D1
  - 验收：软过期成功登录扣减一次，remaining=0 幂等，硬过期阻断不变
  - 实现：`CredentialSecurityService.consumeGraceLoginOnSuccess`（仅过期策略启用且已过期时扣减）接入 `RecordLoginUseCaseService.recordSuccess`。注意：当前仅 ADMIN 触发 recordSuccess，Member 登录记录属 L2，Member baseline 未启用过期策略，天然降级

- [x] T4（部分）：初始密码生成 + 配置（按 D3，见偏差说明）
  - 依赖：D3
  - 已完成：`InitialPasswordService`（RANDOM/FIXED 生成）+ `InitialPasswordPolicy` 配置；PMS/Member `BizUserServiceImpl` 统一改用 `initialPasswordService.generate()`；`oneTime` 由改密清 `mustChangePwd/force_change` 实现；`forceChangeOnFirstLogin` 由现有 `mustChangePwd=true` 满足
  - 待 T5 决策：`validHours` 初始密码超期在登录时的强制拦截（与 T5 同一登录/访问闸口）

- [x] T5：强制改密访问限制（决策：方案 B 受限 scope，且已存在）
  - 依赖：T2、T4
  - **决策**：D2 由「网关拦截」改为「方案 B：登录按 mustChangePwd 签发受限 scope」。
  - 结论：该机制**已在现网实现**——`IdentityUtil.map()`（PMS 与 Member 均有）在 `mustChangePwd=true` 时仅下发 `PermissionConstants.INIT_PASSWORD`；改密接口以 `@AdminOrHasAnyAuthority({INIT_PASSWORD})` 保护，其余接口因缺权限被资源服务拒绝。验收「强制改密态仅白名单接口可访问」已满足，无需新增代码。
  - 后续（非本闭环）：初始密码 `validHours` 硬超期拦截需在登录期读取 `mustChangePwd + passwordChangedAt` 铺设，价值边际，拆为后续增强。

- [x] T6（部分）：Member 侧凭证登录判定对齐（按 D4，见偏差说明）
  - 依赖：D4
  - 已确认：Member 已装配 `AuthContextSupport`（凭证路径已通），且创建/重置/改密均经账号域用例，force_change/mustChangePwd 语义与 ADMIN 一致
  - **偏差**：Member provider 仅依赖 `ingot-security-credential`（NoOp 持久化），未含 `ingot-security-credential-data`。要让 Member 在启用过期/历史策略时与 ADMIN 完全一致，需为 Member 增 `credential-data` 依赖并在 `ingot_member` 建 `password_history`/`password_expiration` 表（DDL）。因涉及 DDL 与 Member baseline 定位，需重新确认是否纳入本闭环或拆分后续

- [x] T7：新增初始密码配置项 `ingot.security.credential.policy.initial-password.*` 并纳入 `local` Nacos 动态刷新
  - 依赖：T4
  - 实现：配置项加入 `CredentialSecurityProperties.PolicyConfig`，`local` 模式随 Nacos 刷新；`InitialPasswordService` 每次读取最新配置

- [x] T8：`local` 策略随 Nacos 刷新重建
  - 依赖：无
  - 实现：`LocalCredentialPolicyLoader` 实现 `ApplicationListener<RefreshScopeRefreshedEvent>`，刷新时 `LocalCompiledPolicyCache.evictAll()`；credential 模块补 `compileOnly spring-cloud-context`

## 验证任务

- [ ] V1：单元测试（宽限扣减边界、force_change 对齐、初始密码过期 / 生成）
- [ ] V2：集成测试（ADMIN 与 Member「创建→首登强制改密→改密→再登录」全链路）
- [ ] V3：降级验证（`mode=local` Nacos 动态刷新）
- [ ] V4：回归（现有 PMS 登录 / 改密、硬过期阻断不受影响）；相关模块编译通过

## 完成检查

- [ ] 实现与 DESIGN 一致
- [ ] REQUIREMENTS 验收标准全部满足
- [ ] Current 已更新（`specs/current/security/credential-security/`）
- [ ] roadmap 状态表 L1 更新为 done，ROADMAP.md 关联更新
- [ ] Change 已记录完成信息并归档
