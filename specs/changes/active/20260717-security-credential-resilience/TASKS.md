# Tasks

## 决策任务（已敲定，对应 DESIGN 决策记录）

- [x] D-H：`validHours` 超期硬拦截落点 → **`AuthContextSupport`（账号域单点）**
- [x] D-I：`fallback.local-floor-enabled` 默认值 → **`true`（可用性优先）**
- [x] D-J：本期安全中心 `INITIAL_PASSWORD` → **仅预留类型 + 远程可读，管理台后续**

## 实施任务

### 阶段一：弹性降级阶梯（核心）

- [x] T1：新增 `CredentialRemoteUnavailableException`，改造 `RemoteCredentialPolicyConfigService.getAll()` 区分「失败抛异常」与「成功空返回空」
  - 依赖：无
  - 验收：失败抛专用异常；成功（含空）返回真实数据；本地 delegate 不受影响
  - 实现：调用异常 / 空响应 / 非成功码统一抛 `CredentialRemoteUnavailableException`；成功含空返回真实数据；provider 本地 delegate 不受影响

- [x] T2：新增 `LastKnownGoodStore`（Redis 独立 key 长存 + 进程内副本），仅远程成功刷新，失效事件不清
  - 依赖：无
  - 验收：save/load 正确；Redis 不可用降级进程内；`InvalidationBus`/刷新事件不清 LKG
  - 实现：独立 key `in:credential:policy:lkg`（无 TTL 长存）+ 进程内 `AtomicReference`；Redis 不可用自动回退进程内副本；`evictAll` 不触及 LKG

- [x] T3：抽取属性→`List<CredentialPolicyConfigVO>` 映射为共享 `LocalFloorSupplier`（复用 `LocalCredentialPolicyLoader` 逻辑），保证安全基线非空
  - 依赖：无
  - 验收：strength/history/expiration/initialPassword 全量映射；缺省安全基线非空
  - 实现：映射四类策略为 VO；启用项才纳入 strength/history/expiration，初始密码始终纳入；全部校验类关闭时补最小强度基线，永不 fail-open

- [x] T4：新增 `ResilientCredentialPolicyConfigService`（remote→LKG→Nacos 地板），装配为 remote 模式最内层 delegate；确保失败态不入 L1/L2
  - 依赖：T1、T2、T3、D-I
  - 验收：成功刷新 LKG；失败走 LKG；无 LKG 走地板；合法空不兜底；失败态不缓存
  - 实现：装配为 `credentialPolicyConfigDelegate`（remote 消费方），成为 L1/L2 之下最内层；失败时返回有效兜底值（不返回失败空），L1/L2 本就不缓存空

- [x] T5：降级可观测——降级日志 + 指标 + actuator/端点暴露「当前生效来源」
  - 依赖：T4
  - 验收：走 LKG/地板时有可采集打点与来源查询
  - 实现：`CredentialPolicySourceHolder`（当前来源 + LKG/地板计数 + 最近降级时间）+ WARN 日志 + actuator 端点 `credentialpolicy`（`@ConditionalOnClass` 守护）。偏差：以 holder 计数经端点暴露替代 Micrometer meter（见 README 差异）

- [~] T6（可选增强）：远程调用接入熔断（Sentinel），打开期间直接走兜底
  - 依赖：T4
  - 结论：本期未实现（P2 可选）。无熔断时以「每次远程失败即走兜底 + 不缓存失败」保证正确性，符合需求 12

### 阶段二：初始密码接入统一抽象

- [x] T7：`CredentialPolicyType` 增 `INITIAL_PASSWORD("4")`；新增不可变载体 `InitialPasswordConfig`
  - 依赖：无
  - 验收：枚举可解析；载体字段与 `InitialPasswordPolicy` 对齐
  - 实现：枚举增 `INITIAL_PASSWORD("4","初始密码")`；`InitialPasswordConfig` record（generation/length/fixedPassword/validHours/oneTime/forceChangeOnFirstLogin）含 `from`/`defaults`

- [x] T8：`CredentialPolicyLoader` 增 `getInitialPasswordConfig()`；`Remote`/`Local` 双实现（Remote 复用降级后的 `getAll()`，`createPolicy(INITIAL_PASSWORD)` 返回 null）
  - 依赖：T7、T4
  - 验收：remote 命中/缺省；local 从属性；初始密码享受同一降级阶梯
  - 实现：Remote 从 `getAll()` 挑 `INITIAL_PASSWORD` 行鲁棒解析（缺失走缺省），`createPolicy(INITIAL_PASSWORD)=null`；Local 从属性构建

- [x] T9：`DefaultInitialPasswordService` 依赖由 `CredentialSecurityProperties` 改为 `CredentialPolicyLoader`；调整 `CredentialSecurityAutoConfiguration` 装配
  - 依赖：T8
  - 验收：`generate/isExpired/isForceChangeOnFirstLogin` 均基于 loader 生效配置；调用方无感
  - 实现：三方法均基于 `loader.getInitialPasswordConfig()`；装配参数由 properties 改为 loader；调用方 `generate()` 签名无变化

### 阶段三：接线遗留能力

- [x] T10：`RegisterUserUseCaseService` 的 `ADMIN_CREATE` 用 `isForceChangeOnFirstLogin()` 决定 `mustChangePwd`（命令显式值优先）
  - 依赖：T9
  - 验收：策略关闭时 `ADMIN_CREATE` 不再强制改密；开启时置位；`SELF_REGISTER` 不变
  - 实现：命令显式值优先 → ADMIN_CREATE 取 `isForceChangeOnFirstLogin()` → SELF_REGISTER 保持默认 `TRUE`

- [x] T11：登录期初始密码 `validHours` 超期硬拦截（按 D-H 落点），命中则阻断并提示需重置
  - 依赖：T9、D-H
  - 验收：`mustChangePwd` 且 `passwordChangedAt+validHours<now` 时登录被拒并引导重置；未超期正常
  - 实现：`AuthContextSupport` 新增可选 `InitialPasswordService` + `UserAccountPort`；硬过期判定后追加初始密码超期判定，命中置 `credentialsNonExpired=false`；依赖缺失时降级跳过

## 验证任务

- [x] V1：单元测试（Resilient 各分支、delegate 失败/空、LocalFloor 映射、loader 初始密码取值、初始密码服务）— 已补并跑通（credential 模块 6 个测试类）
- [ ] V2：集成测试（安全中心宕机 LKG/地板/恢复、合法空、初始密码 remote/local、`forceChangeOnFirstLogin`/`validHours`）— 待运行时验证
- [ ] V3：降级/刷新验证（`mode=local` 无远程调用；Nacos 改值即时生效；LKG 不被失效事件清）— 待运行时验证
- [ ] V4：回归（strength/history/expiration 非降级路径不变；失败态不入热缓存）— 待运行时验证

## 完成检查

- [x] 实现与 DESIGN 一致（差异见 README：dataId、可观测指标实现方式、T6 可选未做）
- [ ] REQUIREMENTS 验收标准全部满足（待 V2-V4 运行时验收）
- [ ] Current 已更新（凭证安全基线：来源/降级/初始密码）
- [ ] Change 已记录完成信息并归档
