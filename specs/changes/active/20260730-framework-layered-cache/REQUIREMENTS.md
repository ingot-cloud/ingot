# Requirements

## 用户场景

### S1 平台开发者新增一个需要缓存的策略能力

**触发**：开发者要为某个新领域（如未来的租户级配额策略）加一份「远端拉取 + 本地缓存 + 远端挂了要能降级」的能力。

**当前结果**：只能翻 credential 或 gateway-rule-client 的源码照抄五到七个类，抄漏一处（如忘记「不缓存空值」或「LKG 不随 evict 清除」）就会引入难以复现的线上问题。LoginFailure 就是这样抄出来的第三份，且抄漏了 L1/L2。

**期望结果**：引入 `ingot-framework/ingot-cache`，用 `LayeredCacheBuilder` 几行装配出完整的分层与降级链；框架已固化所有安全约定，开发者无需理解全部历史决策。

### S2 集群部署下失效广播消息丢失

**触发**：Platform 修改限流规则并广播 `SecurityPolicyInvalidationEvent`，但某个网关节点因网络抖动或 Redis 重连未收到该 Pub/Sub 消息（Redis Pub/Sub 无持久化、无重投）。

**当前结果**：该节点的 `LocalCompiledCache` 无 TTL，会**永久**持有旧规则，直到进程重启或下一次失效事件恰好被收到。集群内不同节点的限流行为长期不一致。

**期望结果**：L1/L2 均有 TTL，最迟在一个 TTL 周期内自动收敛到最新快照。广播失效仍是即时主路径，TTL 只作兜底。

### S3 网关冷启动或全量失效时的远端风暴

**触发**：网关启动，或收到 `SecurityPolicyDomain.ALL` 失效事件后四域同时重新加载。

**当前结果**：四域各自 L1 miss，各自调用共享但无缓存的 `ResilientSnapshotFetcher`，打出最多 4 次相同的 Feign 请求，并各写一次 LKG。安全中心不可用时更放大为 4 次超时等待。

**期望结果**：共享快照层缓存原始 `SecurityPolicySnapshotVO`，一次远端调用供四域复用。

### S4 TTL 到期后限流规则未随之更新

**触发**：共享快照层 TTL 到期并拉到新版本快照。

**当前结果**（引入 TTL 后的新风险）：Sentinel 在请求路径上读的是 `GatewayRuleManager` 中已加载的规则，不碰 SDK 缓存；而 `RateLimitRuleService.getSnapshot()` 只在 `reloadRules()` 内被调用，`reloadRules()` 只在启动与失效事件时触发。因此**单给 ratelimit 加 TTL 是空转**，缓存刷新了但 Sentinel 规则不变。

**期望结果**：共享快照层在载入新值时发出 version 变化回调，ratelimit 域订阅该回调触发 `reloadRules()`。因共享层为四域共用，blacklist 与 challenge 的请求流量会自然刷新它，ratelimit 搭便车即可，无需额外调度线程。

### S5 运维定位缓存来源与降级历史

**触发**：安全中心短暂不可用后恢复，运维需确认各节点当前用的是远端、LKG 还是 Nacos 地板。

**当前结果**：三个模块各有一个 Actuator 端点（`credentialpolicy` / `securitypolicy` / `loginfailurepolicy`），字段结构相似但需分别访问，且新增模块要再写一个。

**期望结果**：各模块端点保持不变（兼容），另由框架提供汇总端点，一次看到所有已注册缓存实例的来源与降级计数。

## 业务规则

### 必须固化到框架的既有语义（P0）

以下均为三份现有实现的共同约定，抽象后不可丢失：

1. **Resilient 位于 L1/L2 之下**。热缓存可以缓存降级值，TTL 到期后自动重试 remote，从而自动恢复。反之若把 Resilient 放在最外层，降级值不受 TTL 约束。
2. **LKG 生命周期独立于热缓存**：仅存 Redis、不设过期、`evictAll()` **不**清除，只在远端成功时覆盖。
3. **不缓存空值**：L1 与 L2 均不写入空集合，防止规则切换窗口期把「空」固化成 stale empty。读到空的 stale key 时主动删除并穿透。
4. **远端失败与合法空快照严格区分**：远端 HTTP 成功但 data 为空是**合法空**，接受、刷新 LKG、不触发降级；只有抛 `RemoteUnavailableException` 才进入降级阶梯。
5. **地板 fail-closed**：远端不可用且无 LKG 时，若地板禁用则**抛异常**而非 fail-open 返回空。地板本身强制非空（缺配置时补最小基线）。
6. **发布方自行 evict 本地**：`RedisInvalidationBus` 按 origin 过滤自身事件，广播方收不到自己的消息，因此必须在广播前先清本地。
7. **失效回调容错**：同一域允许多个 evictor，按注册顺序串行执行，单个异常不影响其他回调。

### 派生缓存的版本语义（P0）

8. gateway 的编译产物（`CompiledIpList` 持有 `java.util.regex.Pattern`、`CompiledChallengePolicy` 持有 `PathPattern`）**不可 JSON 序列化**，只能留在本机，不能进 L2。
9. 派生缓存的失效键必须是 `(source, version)` **二元组**，不能只比 version 数值：地板快照 version 恒为 0，remote version 来自 DB，降级与恢复之间会出现 version 回退甚至相等。仅比数值会导致「降级到地板后恢复 remote 却不重编译」。
10. version 未变化时**不重编译**，且 `GatewayRuleManager.loadRules` 不重复执行，避免 Sentinel 规则周期性抖动。

### 迁移兼容性（P0）

11. 四个消费者的**对外 SPI 签名、配置属性键、Redis key、Actuator endpoint id 全部保持不变**，本次属纯内部实现替换。
12. credential 已归档为 L1 上线基线（[specs/current/security/credential-security](../../../current/security/credential-security/README.md)），迁移必须零行为变化，包含 provider 侧 `LocalCredentialPolicyConfigConfig` 覆盖 delegate（MySQL 直查、不包 Resilient）的装配方式。
13. dict 的 `mode=AUTO|LOCAL|REMOTE|NONE` 语义与多 key `evict(code)` 粒度保持不变。

### 框架可用性（P1）

14. Redis 与 Actuator 为**可选依赖**（`compileOnly` + `ObjectProvider`），Redis 缺失时 L2 与 LKG 静默降级为 no-op，不影响 L1 与地板。
15. 各层可独立开关，任一层关闭后链路仍完整（装饰器缺省即跳过），不得出现「关掉某层导致 Bean 缺失」的耦合——这正是上一轮 [网关策略 SDK 配置解耦](../20260729-security-access-protection/DESIGN.md) 已确立的契约。

## 边界与非目标

- **不覆盖 Spring Cache（`@Cacheable`）体系**：PMS/Member/Auth 的服务层 CRUD 缓存由 `InRedisCacheManager` 承担，场景是「按实体 id 缓存查询结果」，无降级需求，不在本框架职责内。
- **不引入分布式锁防缓存击穿**：现有实现用 `synchronized` 双重检查做进程内单飞，跨进程重复加载可接受（策略快照读多写少，且 L2 会吸收大部分）。
- **不实现主动预热调度**：仅提供可配的 `refresh-interval` 兜底开关，默认关闭。正常部署由请求流量驱动懒刷新。
- **不改 `InvalidationBus`**：其 origin 过滤、channel 命名、`RedisMessageListenerContainer` 单例约束均保持。
- **dict 不新增降级能力**（见 README 非目标）。

## 验收标准

### Phase 01 框架

- [ ] `ingot-framework/ingot-cache` 编译通过并被聚合构建，`settings.gradle` 与 `config/ingot.gradle` 已注册
- [ ] 装饰器组合矩阵单测通过：L1/L2/Resilient 各自开关的全部组合下链路完整且语义正确
- [ ] 业务规则 1-7 各有对应单测（尤其：LKG 不随 evict 清除、不缓存空值、fail-closed、合法空不降级）
- [ ] `VersionedDerivedCache` 单测覆盖 version 回退与 version 相等但 source 不同的场景（规则 9）
- [ ] 多 key `evictAll()` 的 Redis SCAN+DEL 行为单测通过
- [ ] `.agents/skills/layered-cache/SKILL.md` 可独立指导一次新缓存能力的接入，AGENTS.md 已引用

### Phase 02 gateway

- [ ] 冷启动 Feign 调用次数从 4 降为 1（S3）
- [ ] `ALL` 失效事件后 Feign 调用次数为 1
- [ ] 广播失效丢失时，最迟一个 L1 TTL 周期内节点收敛到最新快照（S2）
- [ ] 共享快照 version 未变化时 `GatewayRuleManager.loadRules` 不被调用（规则 10）
- [ ] 共享快照 version 变化时（含 TTL 懒刷新路径）Sentinel 规则被 reload（S4）
- [ ] `local-floor-enabled=false` 且无 LKG 时仍抛 `PolicyRemoteUnavailableException`，未 fail-open
- [ ] Actuator `securitypolicy` 输出字段与迁移前一致
- [ ] 上一轮确立的配置正交性未回退：不设 `ingot.security.policy.client.*` 任何键时 remote 模式可装配；单域关闭不影响其他域

### Phase 03 LoginFailure + credential

- [ ] L4 change 的 T4-1 与 T4-2 已完成（门禁）
- [ ] LoginFailure 失效事件后 L1/L2 被真实清除（修复 D-C）
- [ ] credential 迁移前后 Actuator `credentialpolicy` 输出逐字段一致
- [ ] credential 的 Redis key `in:credential:configs:all` 与 `in:credential:policy:lkg` 内容格式不变
- [ ] credential 配置键 `ingot.security.credential.cache.*` 全部仍生效，默认值不变
- [ ] provider 侧 delegate 覆盖（MySQL 直查、不包 Resilient）行为不变

### Phase 04 dict + 收口

- [ ] dict 多 key `evict(code)` 与 `evictAll()` 粒度不变
- [ ] dict `mode=AUTO|LOCAL|REMOTE|NONE` 四种模式行为不变
- [ ] 四处旧实现类已删除，全仓库无残留引用
- [ ] 全量编译 + 单测通过
- [ ] `specs/current/framework/layered-cache/` 已建立并反映 As-Built
