# 统一分层缓存 SPEC

> 记录当前已验收并在线生效的框架事实。

## 1. 分层顺序

装饰器链自外向内固定，不可配置：

```
刷新通知（可选） → L1 Caffeine → L2 Redis → Resilient / Loader
```

- Resilient 位于 L1/L2 **之下**：降级值受 TTL 约束，远端恢复后自动重试。
- 刷新通知位于**最外层**：监听器回调回读必然命中 L1；仅适用于单 key 共享快照。
- 任一可选层缺省（未配置、开关关闭、Redis 不可用）即跳过，链路仍完整。
- Redis / Actuator 为框架 `compileOnly`；消费模块自行提供运行时依赖。

## 2. SPI

| 类型 | 职责 |
|---|---|
| `LayeredCache<K, V>` | `get` / `evict` / `evictAll` / `evictMatching` / `name` |
| `CacheValueLoader<K, V>` | 最内层加载器；合法空正常返回，不可用抛 `RemoteUnavailableException` |
| `CacheFloorSupplier<K, V>` | 地板；必须返回非空基线 |
| `RemoteUnavailableException` | 远端不可用信号；各模块可继承以保留自身异常类型 |
| `LayeredCacheBuilder` / `LayeredCacheSettings` | 流式装配；配置键归属消费模块，框架只收映射后的 POJO |
| `CacheSource` / `CacheSourceHolder` | `REMOTE` / `LAST_KNOWN_GOOD` / `LOCAL_FLOOR`；仅 Resilient 层更新 |
| `LayeredCacheCoordinator<E, D>` | 订阅 `InvalidationBus`，按域串行 evictor，单回调异常不中断 |
| `VersionedDerivedCache` / `LazyDerivedCache` | 不可序列化编译产物；失效键为 `(source, version)` 或显式 evict |
| `GET /actuator/layeredcache` | 汇总已注册实例；各模块自有端点保留 |

`evictMatching(Predicate<K>, l2ScanPattern)`：L1 按谓词、L2 按 SCAN 模式清除子集。用于字典「按 code 清掉全部 query 变体」。未覆盖的实现默认退化为 `evictAll()`。

同一进程内多个消费模块各自注册**具名** `CacheSourceHolder` Bean，禁止按类型 `@ConditionalOnMissingBean(CacheSourceHolder.class)`，以免 Auth 等进程内计数串台。

## 3. 不可违反的语义

1. Resilient 在最内侧。
2. LKG 独立 Redis key、默认无 TTL、`evict` / `evictAll` / `evictMatching` 均不清除；只在远端成功时写入。
3. 不缓存空值；读到 stale empty 删 key 并穿透。
4. 远端不可用 ≠ 合法空。
5. 地板 fail-closed：无 LKG 且地板关闭时抛异常。
6. 广播方先清本地（`InvalidationBus` 按 origin 过滤回环）。
7. L1 TTL 不可省（Pub/Sub 无重投）。
8. 派生缓存失效键必须是 `(source, version)` 二元组。

## 4. 消费者 As-Built

| 消费者 | 缓存名 | L2 Redis key | LKG Redis key | 配置前缀 | Actuator |
|---|---|---|---|---|---|
| gateway 共享快照 | `security-policy-snapshot` | `in:sec:policy:snapshot` | `in:sec:policy:lkg:snapshot` | `ingot.security.policy.client.cache.*` | `securitypolicy` |
| 登录失败策略 | `login-failure-policy` | `in:sec:lf:policy:snapshot` | `in:sec:lf:policy:lkg` | `ingot.security.access.policy.cache.*` | `loginfailurepolicy` |
| 凭证策略 | `credential` | `in:credential:configs:all` | `in:credential:policy:lkg` | `ingot.security.credential.cache.*` | `credentialpolicy` |
| 字典 | `dict` | `in:dict:items:{code}:{scope}:{tenant}:{app}:{flag}` | 无 | `ingot.dict.client.cache-*` / `redis-*` | 无（仅汇总端点） |
| 会话并发 | `session-concurrency-policy` | `in:sec:session:concurrency` | `in:sec:session:concurrency:lkg` | `ingot.security.session.policy.cache.*` | 无（仅汇总端点） |
| 账号锁定策略 | `account-lockout-policy` | `in:sec:account:policy:snapshot` | `in:sec:account:policy:lkg` | `ingot.security.account.policy.cache.*` | `accountlockoutpolicy` |
| 授权快照 | `authorization-snapshot` | `in:auth:snapshot:{tenantId}:{userId}` | 无 | `ingot.mybatis.scope.*` | 无（仅汇总端点） |

### 4.1 登录失败

`mode=remote` 首次补齐 L1+L2，`evictAll()` 真实清缓存，失效广播不再空转。`mode=local` 仍直接读 Nacos，不走分层缓存。

### 4.2 凭证

对外 SPI `CredentialPolicyConfigService`、配置键、两个 Redis key、`credentialpolicy` 字段与顺序不变。provider 侧 `LocalCredentialPolicyConfigConfig` 覆盖同名 delegate（MySQL 直查），L1/L2 仍叠加且**不**包 Resilient。

### 4.3 字典

`mode=AUTO\|LOCAL\|REMOTE\|NONE` 与 `evict(code)` / `evictAll()` 粒度不变；不启用 LKG/地板。`batchItems` 按键顺序调用 `get`，冷缓存不再折叠为一次批量 RPC，命中 L1/L2 后与迁移前一致。

L2 自定义 `l2(...)` 键映射，按 code 失效走 `evictMatching`。

### 4.4 账号锁定策略

`mode=remote` 装配 L1+L2+Resilient，空列表当远端不可用。`mode=local` 直接读 Nacos，不走分层缓存。失效域 `ACCOUNT_LOCKOUT`。

### 4.5 授权快照

装配 L1+L2，**关闭** Resilient/LKG/地板。空授权不写热缓存。TTL cap 到 30s。写路径事务提交后本节点 `evictAll` 并广播 `authorization.invalidate`。过期刷新失败 fail-closed（503），不以旧授权放行。配置键归属 `ingot.mybatis.scope`，见 [data-authorization](../../pms/data-authorization/SPEC.md)。
