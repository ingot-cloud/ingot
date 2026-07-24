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

- [x] T2：新增 `LastKnownGoodStore`（Redis 独立 key 长存），仅远程成功刷新，失效事件不清
  - 依赖：无
  - 验收：save/load 正确；`InvalidationBus`/刷新事件不清 LKG
  - 实现：独立 key `in:credential:policy:lkg`（无 TTL 长存）；`evictAll` 不触及 LKG
  - 后续修订：见 T13（移除进程内二级副本，Redis 为唯一 LKG 源）

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

### 阶段四：缓存模型整改（弹性阶梯落地修正）

- [x] T12：移除无 TTL 的进程内编译缓存 `LocalCompiledPolicyCache`，使 `loadPolicies()` 与 `getInitialPasswordConfig()` 共用同一链路
  - 依赖：T4、T8、T9
  - 背景：既有 `LocalCompiledPolicyCache` 无 TTL、仅事件失效，`loadPolicies()` 首次编译后永久短路，稳态下不再重进 L1/L2/Resilient/LKG/地板链路；安全中心宕机时失效事件不触发，弹性阶梯对校验主链路近乎失效，且与直连 `getAll()` 的 `getInitialPasswordConfig()` 节奏不一致
  - 决策：保持 LKG 优先、Nacos 地板仅冷启动兜底（不改 `ResilientCredentialPolicyConfigService`/`LastKnownGoodStore` 优先级与 TTL）；移除编译缓存，两条加载路径统一走链路
  - 实现：删除 `LocalCompiledPolicyCache` 类与 bean；`RemoteCredentialPolicyLoader.loadPolicies()` 改为即时 `buildPolicies(getAll())`；`LocalCredentialPolicyLoader` 去掉编译缓存与 `ApplicationListener<NacosConfigRefreshEvent>`，改为每次按属性即时编译（local 模式刷新由 `ConfigurationPropertiesRebinder` 重绑定完成）；`CredentialCacheCoordinator` 与 provider `CredentialInvalidationPublisher` 去掉编译缓存清理，仅保留 L1/L2 evict
  - 验收：`loadPolicies()` 连续调用反映链路变化（不再永久短路）；稳态新鲜度上界 = L1 TTL；宕机经 LKG/地板降级、恢复后自动回远程；local 模式 Nacos 改值下次加载即生效

- [x] T13：移除 LKG 进程内二级副本，Redis 为唯一 LKG 源（对应决策 D-L，一致性优先）
  - 依赖：T2、T4
  - 背景：进程内 `AtomicReference` 副本不可跨节点共享，负载均衡下「Redis LKG 缺失 + 远程宕机」会导致热/冷节点分别用进程内 LKG 与 Nacos 地板，产生跨节点策略分叉且不自愈
  - 实现：`LastKnownGoodStore` 去掉 `localSnapshot`；`save()` 在无 Redis 时为空操作，`load()` 在无 Redis/缺失/异常时返回 `null` → 统一落 Nacos 地板；更新 AutoConfiguration LKG bean Javadoc
  - 顺带加固：`CredentialCacheProperties.l1Ttl/l2Ttl` 增 `@DurationUnit(MINUTES)`，避免无单位数值被按毫秒绑定导致「写入即过期」的缓存假性未命中
  - 验收：Redis 在 → 全集群一致 LKG；Redis 不可用/缺失 → 全集群统一 Nacos 地板；单测以测试替身验证 Resilient 阶梯（LKG 有/无）分支

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

- [x] V1：单元测试（Resilient 各分支、delegate 失败/空、LocalFloor 映射、loader 初始密码取值、初始密码服务；T12 追加 `loadPolicies` 随链路变化的「不再永久短路」断言）— 已补并跑通（credential 模块）
- [x] V2：集成测试（安全中心宕机 LKG/地板/恢复、合法空、初始密码 remote/local、`forceChangeOnFirstLogin`/`validHours`）— 运行时验证通过
- [x] V3：降级/刷新验证（`mode=local` 无远程调用、Nacos 改值下次加载即生效且无编译缓存残留；remote 宕机 ≤L1 TTL 内 `loadPolicies` 经链路降到 LKG；LKG 不被失效事件清）— 运行时验证通过
- [x] V4：回归（strength/history/expiration 非降级路径不变；失败态不入热缓存；`loadPolicies` 与 `getInitialPasswordConfig` 降级/刷新节奏一致）— 运行时验证通过

## 完成检查

- [x] 实现与 DESIGN 一致（差异见 README：dataId、可观测指标实现方式、T6 可选未做、D-K 编译缓存整改、D-L LKG 进程内副本移除）
- [x] REQUIREMENTS 验收标准全部满足（V2-V4 运行时验收通过）
- [x] Current 已更新（凭证安全基线：来源/降级阶梯/初始密码统一抽象/缓存模型/可观测）
- [x] Change 已记录完成信息并归档
