# Tasks

## 决策任务（已敲定，对应 DESIGN 决策记录）

- [x] D-H：`validHours` 超期硬拦截落点 → **`AuthContextSupport`（账号域单点）**
- [x] D-I：`fallback.local-floor-enabled` 默认值 → **`true`（可用性优先）**
- [x] D-J：本期安全中心 `INITIAL_PASSWORD` → **仅预留类型 + 远程可读，管理台后续**

## 实施任务

### 阶段一：弹性降级阶梯（核心）

- [ ] T1：新增 `CredentialRemoteUnavailableException`，改造 `RemoteCredentialPolicyConfigService.getAll()` 区分「失败抛异常」与「成功空返回空」
  - 依赖：无
  - 验收：失败抛专用异常；成功（含空）返回真实数据；本地 delegate 不受影响

- [ ] T2：新增 `LastKnownGoodStore`（Redis 独立 key 长存 + 进程内副本），仅远程成功刷新，失效事件不清
  - 依赖：无
  - 验收：save/load 正确；Redis 不可用降级进程内；`InvalidationBus`/刷新事件不清 LKG

- [ ] T3：抽取属性→`List<CredentialPolicyConfigVO>` 映射为共享 `LocalFloorSupplier`（复用 `LocalCredentialPolicyLoader` 逻辑），保证安全基线非空
  - 依赖：无
  - 验收：strength/history/expiration/initialPassword 全量映射；缺省安全基线非空

- [ ] T4：新增 `ResilientCredentialPolicyConfigService`（remote→LKG→Nacos 地板），装配为 remote 模式最内层 delegate；确保失败态不入 L1/L2
  - 依赖：T1、T2、T3、D-I
  - 验收：成功刷新 LKG；失败走 LKG；无 LKG 走地板；合法空不兜底；失败态不缓存

- [ ] T5：降级可观测——降级日志 + 指标 + actuator/端点暴露「当前生效来源」
  - 依赖：T4
  - 验收：走 LKG/地板时有可采集打点与来源查询

- [ ] T6（可选增强）：远程调用接入熔断（Sentinel），打开期间直接走兜底
  - 依赖：T4
  - 验收：安全中心持续不可用时不逐请求打死接口

### 阶段二：初始密码接入统一抽象

- [ ] T7：`CredentialPolicyType` 增 `INITIAL_PASSWORD("4")`；新增不可变载体 `InitialPasswordConfig`
  - 依赖：无
  - 验收：枚举可解析；载体字段与 `InitialPasswordPolicy` 对齐

- [ ] T8：`CredentialPolicyLoader` 增 `getInitialPasswordConfig()`；`Remote`/`Local` 双实现（Remote 复用降级后的 `getAll()`，`createPolicy(INITIAL_PASSWORD)` 返回 null）
  - 依赖：T7、T4
  - 验收：remote 命中/缺省；local 从属性；初始密码享受同一降级阶梯

- [ ] T9：`DefaultInitialPasswordService` 依赖由 `CredentialSecurityProperties` 改为 `CredentialPolicyLoader`；调整 `CredentialSecurityAutoConfiguration` 装配
  - 依赖：T8
  - 验收：`generate/isExpired/isForceChangeOnFirstLogin` 均基于 loader 生效配置；调用方无感

### 阶段三：接线遗留能力

- [ ] T10：`RegisterUserUseCaseService` 的 `ADMIN_CREATE` 用 `isForceChangeOnFirstLogin()` 决定 `mustChangePwd`（命令显式值优先）
  - 依赖：T9
  - 验收：策略关闭时 `ADMIN_CREATE` 不再强制改密；开启时置位；`SELF_REGISTER` 不变

- [ ] T11：登录期初始密码 `validHours` 超期硬拦截（按 D-H 落点），命中则阻断并提示需重置
  - 依赖：T9、D-H
  - 验收：`mustChangePwd` 且 `passwordChangedAt+validHours<now` 时登录被拒并引导重置；未超期正常

## 验证任务

- [ ] V1：单元测试（Resilient 各分支、delegate 失败/空、LocalFloor 映射、loader 初始密码取值、初始密码服务）
- [ ] V2：集成测试（安全中心宕机 LKG/地板/恢复、合法空、初始密码 remote/local、`forceChangeOnFirstLogin`/`validHours`）
- [ ] V3：降级/刷新验证（`mode=local` 无远程调用；Nacos 改值即时生效；LKG 不被失效事件清）
- [ ] V4：回归（strength/history/expiration 非降级路径不变；失败态不入热缓存）

## 完成检查

- [ ] 实现与 DESIGN 一致
- [ ] REQUIREMENTS 验收标准全部满足
- [ ] Current 已更新（凭证安全基线：来源/降级/初始密码）
- [ ] Change 已记录完成信息并归档
