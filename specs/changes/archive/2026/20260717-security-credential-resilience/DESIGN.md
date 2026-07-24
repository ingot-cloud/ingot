# Design

## 方案摘要

在既有「L1 Caffeine → L2 Redis → delegate(Feign/本地 Mapper)」链路的 **delegate 层之下**插入一个弹性兜底装饰器，把「远程数据源」从「一次 Feign 调用」升级为「远程 → LKG → Nacos 地板」的降级阶梯；同时把初始密码能力从「直连属性」改为「经 `CredentialPolicyLoader` 取生效配置」，使其与 strength/history/expiration 共享同一来源与降级语义。

核心原则：**远程失败可识别 → 不 fail-open → LKG 优先 → Nacos 兜底 → 可观测**。

### 目标分层（`mode=remote`）

```
CredentialPolicyLoader(Remote)
  └─ CredentialPolicyConfigService（对外 @Primary）
       └─ L1 Caffeine（短 TTL，性能；不缓存失败）
            └─ L2 Redis（短 TTL，性能；不缓存失败）
                 └─ ResilientCredentialPolicyConfigService  ← 新增（兜底阶梯）
                      ├─ RemoteCredentialPolicyConfigService（Feign；失败抛异常，空=合法空）
                      ├─ LastKnownGoodStore（Redis 独立 key，长存/不过期）
                      └─ LocalFloorSupplier（CredentialSecurityProperties → List<VO>）
```

- 成功（含空）：返回远程结果，并**刷新 LKG**（含空覆盖）。
- 失败（超时/连接/非成功码/异常）：`RemoteCredentialPolicyConfigService` 抛 `CredentialRemoteUnavailableException`；`Resilient` 捕获 → 有 LKG 用 LKG，否则用 Nacos 地板；**绝不返回「失败空」**。
- L1/L2 只缓存「正常返回值」（新鲜或 LKG），失败态不入热缓存（异常直接穿透到 Resilient 兜底后再返回有效值）。

`mode=local`：装配 `LocalCredentialPolicyLoader`，完全不经上述远程链路，只读 `CredentialSecurityProperties`（Nacos）。

## 数据模型与接口

### 1. 远端 delegate 改造（区分失败与空）—— 关键前提

`ingot-security-credential/internal/RemoteCredentialPolicyConfigService.getAll()` 当前把失败与空都返回 `List.of()`。改为：

- 成功（`response.isSuccess()`）：返回 `data`（可能空集合）——表达**合法空**。
- 失败（`response==null` / `!isSuccess()` / 抛异常）：抛 `CredentialRemoteUnavailableException`（新增，`RuntimeException`）。

> 注意：`ingot-security-provider` 进程内以本地 Mapper 覆盖该 delegate（`LocalCredentialPolicyConfigService`），本地实现无远程失败语义，保持返回真实数据/空即可。

### 2. 新增 `ResilientCredentialPolicyConfigService`（兜底阶梯）

`implements CredentialPolicyConfigService`，构造依赖：raw delegate、`LastKnownGoodStore`、`LocalFloorSupplier`、开关配置。

```
getAll():
  try:
    data = delegate.getAll()          // 成功（含空）
    lkg.save(data)                    // 刷新 LKG（含空）
    return data
  catch CredentialRemoteUnavailableException:
    metricDegrade("remote-unavailable")
    lkgData = lkg.load()
    if lkgData != null:
        markSource(LAST_KNOWN_GOOD); return lkgData
    markSource(LOCAL_FLOOR); return localFloor.get()   // Nacos 地板
evictAll(): 仅透传给上层热缓存清理；不清 LKG
```

### 3. `LastKnownGoodStore`（LKG 快照）

- 存储：Redis `StringRedisTemplate`，独立 key（如 `ingot:credential:policy:lkg`，与 `CredentialCacheProperties.l2KeyPrefix` 区分命名空间），**长 TTL 或不过期**。
- **Redis 为 LKG 唯一存储，不持进程内副本**（决策 D-L）：多节点从同一 key 读取，降级来源跨节点一致。
- 仅 `ResilientCredentialPolicyConfigService` 在远程成功时写入。
- 失效事件（`InvalidationBus`；`mode=local` 的 Nacos 变更由 `ConfigurationPropertiesRebinder` 重绑定属性）**不清 LKG**，只清 L1/L2（`mode=remote`）或重绑定属性（`mode=local`）。
- Redis 不可用（或 key 缺失）时：`load()` 返回 `null` → 统一落 Nacos 地板，不再有进程内兜底。

### 4. `LocalFloorSupplier`（Nacos 地板）

- 将 `CredentialSecurityProperties.policy`（strength/history/expiration/initialPassword）映射为 `List<CredentialPolicyConfigVO>`，供 Resilient 在无 LKG 时兜底。
- 复用/抽取现有 `LocalCredentialPolicyLoader` 的属性→策略映射逻辑到共享 mapper，避免重复。
- 保证「D-E：Nacos 地板非空」由该 supplier 的缺省安全基线 + 部署方 Nacos 配置共同保证。

### 5. 配置项

`CredentialSecurityProperties.policy` 下新增（`ingot.security.credential.policy.*`）：

```yaml
ingot:
  security:
    credential:
      policy:
        mode: remote          # local(强制本地) | remote(启用降级阶梯)
        fallback:
          local-floor-enabled: true   # 无 LKG 时是否落 Nacos 地板（false 则无 LKG 时抛错，极端严格场景）
        # 初始密码（既有，local 兜底源）
        initial-password:
          generation: RANDOM
          length: 10
          fixed-password: "******"
          valid-hours: 72
          one-time: true
          force-change-on-first-login: true
```

- 不加 `@RefreshScope`；`@ConfigurationProperties` 由 `ConfigurationPropertiesRebinder` 在配置变更时原地 rebind。

### 6. 初始密码接入统一抽象（D-G）

**6.1 策略类型**：`CredentialPolicyType` 增 `INITIAL_PASSWORD("4", "初始密码")`。

**6.2 Loader 接口**：`CredentialPolicyLoader` 增方法：

```java
InitialPasswordConfig getInitialPasswordConfig();   // 返回生效的初始密码配置（remote 优先/local 兜底）
```

- 新增不可变载体 `InitialPasswordConfig`（generation/length/fixedPassword/validHours/oneTime/forceChangeOnFirstLogin）。
- `RemoteCredentialPolicyLoader`：从 `policyConfigService.getAll()`（已含降级阶梯）挑 `INITIAL_PASSWORD` 行 → 映射为 `InitialPasswordConfig`；缺失则返回缺省。`loadPolicies()` 的 `createPolicy` 对 `INITIAL_PASSWORD` 返回 `null`（不参与校验策略列表）。
- `LocalCredentialPolicyLoader`：从 `properties.getPolicy().getInitialPassword()` 构建。

**6.3 服务改造**：`DefaultInitialPasswordService` 依赖从 `CredentialSecurityProperties` 改为 `CredentialPolicyLoader`；`generate()` / `isExpired()` / `isForceChangeOnFirstLogin()` 均基于 `loader.getInitialPasswordConfig()`。`CredentialSecurityAutoConfiguration.initialPasswordService(...)` 装配参数随之调整。

### 7. 接线遗留能力

- **`isForceChangeOnFirstLogin`**：`RegisterUserUseCaseService` 的 `ADMIN_CREATE` 分支，`mustChangePwd` 缺省值由硬编码 `TRUE` 改为「命令显式值优先，否则取 `initialPasswordService.isForceChangeOnFirstLogin()`」。`SELF_REGISTER` 行为不变。
- **`validHours`（`isExpired`）**：在登录期，对「`mustChangePwd=true`（仍持初始/重置密码）且 `passwordChangedAt + validHours < now`」判定为初始密码超期 → 硬阻断登录并提示需管理员重置。
  - 落点建议：账号域 `AuthContextSupport`（单一共享点），新增 `ObjectProvider<InitialPasswordService>` 与账号 `mustChangePwd/passwordChangedAt` 的读取（经现有 `UserAccountPort` 或在 `fill` 入参补充），命中则置 `credentialsNonExpired=false`。
  - 备选落点：`IdentityUtil.map()`（PMS/Member 各一处，已持有实体的 `mustChangePwd/passwordChangedAt`），命中则不下发 `INIT_PASSWORD` scope 而返回不可用。**落点在 TASKS 作为决策点 D-H 待定**。

## 数据流与失败处理

### 远程正常
`loadPolicies()/getInitialPasswordConfig()` → L1 命中直接返回；未命中 → L2 → Resilient → Feign 成功 → 刷新 LKG → 回填 L1/L2。

### 远程失败（有 LKG）
Feign 抛 `CredentialRemoteUnavailableException` → Resilient 捕获 → 返回 LKG（标记来源 LAST_KNOWN_GOOD，打点+日志）→ L1/L2 短 TTL 缓存该有效值 → 远程恢复后下次未命中即回新鲜值。

### 远程失败（无 LKG，冷启动）
Resilient → `local-floor-enabled=true` → 返回 Nacos 地板（标记 LOCAL_FLOOR）；`=false` → 抛错由上层处理（极端严格场景，非默认）。

### 合法空
Feign 成功返回空 → 直接返回空 + 刷新 LKG 为空 → 上层按「无该类策略」执行（校验放行 / 初始密码用缺省）。**不**兜底。

### 边界
- Redis 不可用：L2 跳过（现状即如此）；LKG 亦不可用（`load()` 返回 `null`）→ 统一落 Nacos 地板。
- 并发刷新 LKG：以最后一次成功为准，允许覆盖，最终一致。
- `mode=local`：不触达任何远程/LKG，纯 Nacos。

## 迁移与回滚

- **DDL**：安全中心 `credential_policy_config` 若以 `policy_type` 约束枚举，需允许新值 `4`（`INITIAL_PASSWORD`）。若为自由字符串则无需 DDL。上线前确认并按需补迁移脚本 + 回滚。
- **数据**：无强制回填；安全中心未配置初始密码策略时，`remote` 走缺省、`local` 走 Nacos，行为兼容 L1 现状。
- **兼容**：
  - `mode` 缺省仍为 `local`，未启用 remote 的服务行为完全不变。
  - delegate 由「失败返回空」改为「失败抛异常」：仅在 `remote` 模式且未包裹 Resilient 时有行为差异；本次同时引入 Resilient，保证对外表现只增强不回退。
  - `DefaultInitialPasswordService` 依赖变更为内部装配调整，对调用方（`generate()`）签名无影响。
- **回滚**：各项为装饰器/新增类 + 装配调整，回退代码即恢复原链路；LKG/新增配置项为增量，无数据不可逆操作。新增 `INITIAL_PASSWORD` 枚举回滚需确认无存量该类型数据。

## 测试策略

- 单元：
  - `ResilientCredentialPolicyConfigService`：成功刷新 LKG / 失败走 LKG / 无 LKG 走地板 / 合法空不兜底 / `local-floor-enabled=false` 抛错。
  - delegate 失败抛异常、成功空返回空。
  - `LocalFloorSupplier` 属性→VO 映射与安全基线非空。
  - `RemoteCredentialPolicyLoader.getInitialPasswordConfig` 命中/缺省；`createPolicy(INITIAL_PASSWORD)==null`。
  - `DefaultInitialPasswordService` 经 loader 取值（remote/local）。
- 集成：
  - `mode=remote` 模拟安全中心宕机：LKG 命中与冷启动地板两条路径；恢复后回新鲜值。
  - 安全中心清空策略：合法空生效 + LKG 刷新为空。
  - 初始密码 remote 下发 vs local 兜底；`forceChangeOnFirstLogin` 与 `validHours` 生效。
- 降级/刷新：`mode=local` 无远程调用；Nacos 改值即时生效（含 `initial-password.*`），LKG 不被失效事件清除。
- 回归：strength/history/expiration 非降级路径行为不变；L1/L2 不缓存失败态。

## 决策记录（已敲定）

- **D-H：`validHours` 初始密码超期硬拦截落点** → **`AuthContextSupport`（账号域单点）**。在其中新增 `ObjectProvider<InitialPasswordService>` 与账号 `mustChangePwd/passwordChangedAt` 读取，命中超期则置 `credentialsNonExpired=false` 硬阻断，提示需管理员重置。不采用 `IdentityUtil` 双点方案。
- **D-I：`fallback.local-floor-enabled` 默认值** → **`true`（可用性优先）**。无 LKG 冷启动时落 Nacos 地板；`false`（严格拒绝）仅作为可配置的极端选项保留。
- **D-J：安全中心 `INITIAL_PASSWORD` 下发** → **仅预留类型 + 远程可读**（`RemoteCredentialPolicyLoader.getInitialPasswordConfig` 可读安全中心该类型行，缺失走缺省）；安全中心侧管理台的可视化配置**后续单列**，不在本期。

- **D-L：LKG 进程内二级副本取舍（实施期讨论）** → **移除进程内副本，Redis 为 LKG 唯一存储（一致性优先）**。
  - 问题：`LastKnownGoodStore` 原持进程内 `AtomicReference` 作为「Redis 也不可用」时的二级兜底。但进程内状态不可跨节点共享——负载均衡下若 Redis LKG 缺失（误删/驱逐/抖动）+ 远程宕机，热节点命中进程内 LKG、冷节点（刚重启）落 Nacos 地板，产生**跨节点策略分叉**，且不自愈（远程恢复前持续）。
  - 权衡：进程内 LKG 的增量收益（Redis 不可用 + 远程宕机 + L1 已过期这一窄窗口里，热节点多保留真实策略）很小，却引入不可共享 → 分叉的复杂度。按 keep_lkg，Nacos 地板本就是合法安全兜底。
  - 决策：移除进程内副本。Redis 在 → 全集群一致 LKG；Redis 不可用/key 缺失 → 全集群统一落 Nacos 地板。`load()` 在无 Redis 时返回 `null`，`save()` 为空操作。降级来源在多节点间可预测、一致，消除分叉。

- **D-K：编译缓存模型整改（实施期发现，弹性阶梯落地修正）** → **移除 `LocalCompiledPolicyCache`，两条加载路径统一走链路**。
  - 问题：既有 `LocalCompiledPolicyCache` 为无 TTL、仅事件失效的进程内编译缓存，位于整条链路之上。`RemoteCredentialPolicyLoader.loadPolicies()` 首次编译后即永久短路，稳态下不再重进 L1/L2/Resilient/LKG/地板；而失效事件仅由安全中心「写」提交后广播，宕机期不触发，导致本 change 的弹性阶梯对校验主链路近乎失效。且新增的 `getInitialPasswordConfig()` 直连 `getAll()`，与 `loadPolicies()` 节奏不一致。
  - 决策：**保持 LKG 优先、Nacos 地板仅冷启动兜底**（不改 `ResilientCredentialPolicyConfigService`/`LastKnownGoodStore` 优先级与 TTL）；**移除编译缓存**，`loadPolicies()` 每次经 `getAll()` 即时编译，与 `getInitialPasswordConfig()` 同源同节奏。
  - 影响：稳态新鲜度上界 = L1 TTL（5min），配置变更仍由 `CredentialInvalidationEvent` 即时清 L1/L2；宕机 ≤L1 TTL 内即经链路降到 LKG，恢复后自动回远程。`mode=local` 去掉 `ApplicationListener<NacosConfigRefreshEvent>`，改为每次按属性即时编译，Nacos 刷新由 `ConfigurationPropertiesRebinder` 重绑定属性完成（行为等价且更直接）。per-call 重编译仅涉及约 3 个策略对象，成本可忽略。
