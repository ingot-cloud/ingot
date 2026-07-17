# Requirements

## 用户场景

### S1 安全中心抖动/宕机时的策略可用性

- 使用者：接入 `ingot-security-credential` 的各业务服务（PMS / Member / Auth 等），`mode=remote`。
- 触发条件：安全中心（`ingot-security`）不可用——连接失败 / 超时 / 返回非成功码 / 抛异常。
- 期望结果：
  - 优先使用**最近一次成功拉取的远程快照（LKG）**继续执行校验与初始密码逻辑。
  - LKG 不存在（冷启动从未成功）时，落到 **Nacos 本地安全基线**。
  - **任何情况下都不因远程失败而退化为「无任何策略」**（不 fail-open）。
  - 恢复后自动回到远程新鲜值，无需重启。

### S2 管理员经安全中心清空某类策略

- 使用者：安全中心管理员。
- 触发条件：在安全中心正常在线时，删除/停用某类策略，`getPolicyConfigs` 成功返回不含该类型（或整体为空）。
- 期望结果：该「成功的空/缺省」被视为**合法的无策略**并即时生效，同时刷新为新的 LKG；**不**被误判为失败去兜底。

### S3 应急强制本地

- 使用者：运维。
- 触发条件：安全中心维护窗口 / 需完全脱离远程。
- 期望结果：`mode=local` 时只读 Nacos，绝不发起远程调用；行为可预测。

### S4 初始密码策略由安全中心统管（可降级）

- 使用者：管理员创建账号 / 重置密码链路（PMS `BizUserServiceImpl`、Member `BizUserServiceImpl`）。
- 触发条件：签发初始密码、判定首登强制改密、判定初始密码有效期。
- 期望结果：
  - `remote` 模式下初始密码策略（生成方式 / 长度 / 固定密码 / `validHours` / `oneTime` / `forceChangeOnFirstLogin`）来自安全中心，并享受 S1 的降级兜底。
  - `local` 模式下来自 Nacos 属性。
  - `forceChangeOnFirstLogin` 真正决定 `ADMIN_CREATE` 是否置 `mustChangePwd`；`validHours` 真正在登录期拦截超期初始密码。

### S5 降级可观测

- 使用者：运维 / 监控。
- 触发条件：任意降级发生（走 LKG 或 Nacos 地板）。
- 期望结果：有明确日志、可采集指标，且可通过 actuator/端点查询「当前生效来源（remote / last-known-good / local-floor）」。

## 业务规则

1. 降级阶梯固定为 `remote(新鲜) → LKG → Nacos 地板`，且**永不 fail-open**。（P0）
2. 兜底仅由**远程调用失败**触发；**成功返回空**为合法无策略，直接接受并刷新 LKG。（P0）
3. 远端 delegate 必须能**区分失败与空**：失败以可识别方式上报（抛专用异常/带状态），空以空集合表达成功。（P0）
4. LKG 与 L1/L2 热缓存**物理与生命周期分离**：LKG 长存/不过期、独立命名空间，仅远程成功时刷新（含成功空）；失效事件只清热缓存，不清 LKG。（P0）
5. **失败结果不得写入热缓存**（避免把失败/降级态缓存整个 TTL）。（P0）
6. `mode=local` 为强制本地，绝不发起远程调用；`mode=remote` 启用降级阶梯。（P0）
7. Nacos 地板必须是**安全基线**（非空），保证 D-B 语义下不退回 fail-open。（P0）
8. 初始密码策略与 strength/history/expiration **共用** `CredentialPolicyLoader`/`CredentialPolicyConfigService` 抽象与降级语义；`DefaultInitialPasswordService` 不再直连 `CredentialSecurityProperties`。（P0）
9. 新增 `INITIAL_PASSWORD` 策略类型：安全中心可下发；无下发时按 local/Nacos 缺省。（P1）
10. `@ConfigurationProperties` 依赖 rebinder 刷新，不加 `@RefreshScope`；派生缓存（编译策略、LKG 除外）在刷新/失效时清理。（P1）
11. 降级发生必须可观测（日志 + 指标 + 当前来源查询）。（P1）
12. 熔断器为可选增强；缺失时以「每次远程失败即走兜底 + 不缓存失败」保证正确性。（P2）

## 边界与非目标

- 不做基于注册中心健康检查的「在线探活」式来源切换，仅以调用结果驱动。
- 不改安全中心策略管理台 UI。
- 不在本次实现 Member 完整凭证持久化（承接 L1 拆出项）。
- LKG 的一致性以「最终一致」为准：远程恢复后下次成功拉取即刷新，不追求强一致。
- 误清空的治理（操作审计 / 二次确认）不在本次范围（成功空覆盖 LKG 属预期）。

## 验收标准

- [ ] `mode=remote` 下安全中心不可用：有 LKG 时用 LKG，无 LKG 时用 Nacos 地板，策略非空可执行，无 fail-open
- [ ] 安全中心恢复后自动回到远程新鲜值，无需重启
- [ ] 安全中心成功返回空：按合法无策略生效，且刷新为新 LKG，不触发兜底
- [ ] 远程失败结果不被写入 L1/L2 热缓存；LKG 不被失效事件清除
- [ ] `mode=local`：无任何远程调用，仅读 Nacos
- [ ] 初始密码在 `remote` 模式来自安全中心并可降级；`local` 模式来自 Nacos
- [ ] `forceChangeOnFirstLogin` 决定 `ADMIN_CREATE` 的 `mustChangePwd`；`validHours` 在登录期拦截超期初始密码
- [ ] 降级发生有日志 + 指标，可查询当前生效来源
- [ ] `CredentialSecurityProperties` 无 `@RefreshScope`，Nacos 改值即时生效（含初始密码 local 配置）
- [ ] 现有 strength/history/expiration 校验行为在非降级路径不回归
