# 凭证策略降级兜底与初始密码收口对齐

> 状态：draft

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
- D-F：`@ConfigurationProperties` 走 rebinder 自动刷新，**不加 `@RefreshScope`**；只需在刷新/失效时清热缓存与编译缓存（沿用 `RefreshScopeRefreshedEvent` + `InvalidationBus`）。
- D-G：初始密码与 strength/history/expiration 统一走 `CredentialPolicyLoader`/`CredentialPolicyConfigService` seam，共享同一降级语义。

## 工件

- [需求](./REQUIREMENTS.md)
- [设计](./DESIGN.md)
- [任务](./TASKS.md)

## 完成记录

- 完成日期：
- 关联提交或 PR：
- 更新的 current capability：
- 与原设计的差异：
- 取消原因：
