# 凭证策略降级兜底与初始密码收口对齐

> 状态：implementing（代码完成，V1 单测通过，待 V2-V4 运行时验收后归档）

## 元数据

| 项 | 值 |
|---|---|
| Change ID | `20260717-security-credential-resilience` |
| 领域 | `security` |
| 负责人 | jy |
| 创建日期 | 2026-07-17 |
| 目标发布日期 | TBD |
| 关联前序 | `20260717-security-credential-closure`（L1）|

## 目标

把凭证策略配置的「来源与降级」从当前的**静态模式切换**升级为**企业级弹性降级**，并让 L1 遗留的初始密码能力真正接入同一套「安全中心优先、Nacos 兜底」的抽象，消除以下问题：

1. `remote` 模式下安全中心不可用时，`RemoteCredentialPolicyConfigService.getAll()` 把「调用失败」和「合法空」都吞成空列表，导致策略**静默全部失效（fail-open）**，且空结果被 L1/L2 缓存。
2. 缺少真正的降级兜底：无「最近成功快照（LKG）」，无 Nacos 地板，无法保证安全中心抖动/宕机期间的安全基线。
3. 初始密码策略（`InitialPasswordService` / `CredentialSecurityProperties.policy.initialPassword`）**直连降级源**，绕过 `CredentialPolicyLoader` 抽象；即使 `remote` 模式也不受安全中心管控。
4. `InitialPasswordService.isExpired` / `isForceChangeOnFirstLogin` 无调用方；`validHours` / `oneTime` / `forceChangeOnFirstLogin` 配置项为「死配置」。

## 范围

**包含：**

- 弹性降级阶梯：`remote(新鲜) → LKG(最近成功快照) → Nacos(地板)`，永不 fail-open。
- 远端 delegate 改造：区分「失败」与「合法空」，失败才触发兜底，空按合法无策略接受。
- LKG 持久快照（Redis 独立命名空间、长存/不过期，仅在远程成功时刷新，含成功空）与热缓存分离。
- 强制本地开关（`mode=local`）语义保留与明确化。
- 降级可观测性：降级触发指标 / 日志 / actuator 暴露「当前生效来源」。
- 初始密码接入统一抽象：新增 `INITIAL_PASSWORD` 策略类型，安全中心可下发；`CredentialPolicyLoader` 暴露初始密码配置（remote/local 双实现）；`DefaultInitialPasswordService` 改为经 loader 取生效配置。
- 接线遗留能力：`isForceChangeOnFirstLogin` 驱动 `ADMIN_CREATE` 的 `mustChangePwd`；`validHours` 初始密码登录超期拦截。

**不包含：**

- 运行时自动探活「安全中心是否在线」的服务发现级切换（本次以调用失败为降级触发）。
- 安全中心侧策略管理台的可视化改造。
- 熔断器接入为可选增强（见 DESIGN），非本次必需。
- Member 完整凭证持久化（承接 L1 拆出项，另立 change）。

## 已锁定决策（评审对齐）

- D-A：兜底目标为 **A（Last-Known-Good）**，LKG 不可用/冷启动时再落 **Nacos 地板**；两者皆不可用时**绝不** fail-open 到「无策略」。
- D-B：远程**成功返回空 = 合法无策略**，直接接受并刷新为新的 LKG，不触发兜底。
- D-C：兜底仅由**远程调用失败**（超时 / 连接失败 / 非成功码 / 异常）触发。
- D-D：LKG 与 L1/L2 热缓存**分离**，LKG 用长存/不过期、独立命名空间，仅远程成功时刷新。
- D-E：Nacos 地板必须维护**安全基线**（不得为空），否则 D-B 语义下会退回 fail-open。
- D-F：`@ConfigurationProperties` 走 rebinder 自动刷新，**不加 `@RefreshScope`**；只需在刷新/失效时清热缓存与编译缓存（`local` 模式由 `LocalCredentialPolicyLoader` 监听 `NacosConfigRefreshEvent` 且仅处理 `in-security-policy.yml`（常量 `NacosConstants.IN_SECURITY_POLICY`，此前文档笔误写作 `in-security-credential.yml`，以代码为准）；`remote` 模式沿用 `InvalidationBus`）。
- D-G：初始密码与 strength/history/expiration 统一走 `CredentialPolicyLoader`/`CredentialPolicyConfigService` seam，共享同一降级语义。
- D-H：`validHours` 初始密码超期硬拦截落点 = **`AuthContextSupport`（账号域单点）**。
- D-I：`fallback.local-floor-enabled` 默认 = **`true`（可用性优先）**。
- D-J：安全中心 `INITIAL_PASSWORD` 本期 = **仅预留类型 + 远程可读，管理台后续**。

## 工件

- [需求](./REQUIREMENTS.md)
- [设计](./DESIGN.md)
- [任务](./TASKS.md)

## 完成记录

- 完成日期：代码完成 2026-07-20（V1 单测通过，待 V2-V4 运行时验收）
- 关联提交或 PR：TBD
- 更新的 current capability：待验收后更新 `specs/current/security/credential-security`（补来源/降级阶梯/初始密码统一抽象）
- 单元测试（V1，已补并通过，credential 模块）：
  - `ResilientCredentialPolicyConfigServiceTest`（成功刷新 LKG / 失败走 LKG / 无 LKG 走地板 / 合法空不兜底 / 地板禁用抛错 / evict 透传）
  - `RemoteCredentialPolicyConfigServiceTest`（失败抛异常 vs 成功空返回空）
  - `LocalFloorSupplierTest`（映射 + 基线非空兜底）
  - `RemoteCredentialPolicyLoaderTest`（`getInitialPasswordConfig` 命中/缺省；`loadPolicies` 排除 `INITIAL_PASSWORD`）
  - `DefaultInitialPasswordServiceTest`（改为经 loader 取值）
- 与原设计的差异：
  - Nacos dataId：DESIGN/D-F 早期写作 `in-security-credential.yml`，实际代码（`local` 模式刷新过滤）使用 `in-security-policy.yml`（常量 `NacosConstants.IN_SECURITY_POLICY`）；以代码与 current SPEC 为准。
  - T5 可观测：以 `CredentialPolicySourceHolder`（当前来源 + 降级计数）经 actuator 端点 `credentialpolicy` 暴露替代 Micrometer meter，避免框架模块强依赖 micrometer；WARN 日志保留。后续如需 Micrometer 指标可增量接入。
  - T6 熔断（Sentinel）为 P2 可选增强，本期未实现；以「每次远程失败即兜底 + 不缓存失败」保证正确性。
  - 弹性装配落点：将 `ResilientCredentialPolicyConfigService` 包裹在 `credentialPolicyConfigDelegate`（remote 消费方）内，成为 L1/L2 之下最内层 delegate；`ingot-security-provider` 的本地 Mapper delegate 不包裹（无远程失败语义）。
  - 兜底值会被 L1/L2 按短 TTL 缓存，远程恢复后随缓存过期回到新鲜值（符合 DESIGN 的最终一致预期）。
- 取消原因：—
