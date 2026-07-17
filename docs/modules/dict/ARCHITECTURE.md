# 字典模块架构设计

> 本文阐述字典模块的分层架构、三级缓存装饰器链、跨节点失效广播流程及关键设计决策。

## 1. 分层视图

```
┌──────────────────────────────────────────────────────────────────┐
│ 业务方（任意微服务） @Autowired DictService                          │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼  Spring 注入
┌──────────────────────────────────────────────────────────────────┐
│  ingot-dict-client                                                │
│  ──────────────────────────────────────────────────────────────  │
│   DictService (@Primary)                                          │
│      └─ CaffeineDictService          ← L1 进程内缓存               │
│            └─ RedisDictService       ← L2 集群共享缓存             │
│                  └─ delegate (dictDelegate bean)                  │
│                        ├─ LocalDictService（PMS 进程内）           │
│                        └─ RemoteDictService（其它服务，Feign）     │
│                                                                   │
│   DictCacheCoordinator   ←── 订阅 InvalidationBus，回调 evict      │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼ subscribes
┌──────────────────────────────────────────────────────────────────┐
│  ingot-event-bus                                                 │
│  ──────────────────────────────────────────────────────────────  │
│   InvalidationBus (SPI)                                           │
│      └─ RedisInvalidationBus         ← 默认实现（Redis Pub/Sub）   │
│   InvalidationEvent + @EventType                                  │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼ publishes
┌──────────────────────────────────────────────────────────────────┐
│  PMS（写端）                                                       │
│  ──────────────────────────────────────────────────────────────  │
│   PlatformDictServiceImpl                                         │
│      ├─ create/update/delete/changeStatus/batchSort               │
│      └─ ApplicationEventPublisher.publishEvent(                   │
│              DictChangedSpringEvent)                              │
│                                                                   │
│   DictInvalidationPublisher (@TransactionalEventListener)        │
│      ├─ origin 端先 redisLayer.evict(...)                          │
│      └─ invalidationBus.publish(DictInvalidationEvent)             │
└──────────────────────────────────────────────────────────────────┘
```

---

## 2. 装饰器链与组合工厂

字典客户端使用经典装饰器模式，由 `DictServiceFactory` 自外向内组合：

```
Caffeine (L1) → Redis (L2) → delegate (Local | Remote)
```

```93:109:ingot-framework/ingot-dict-client/src/main/java/com/ingot/framework/dict/client/config/DictClientAutoConfiguration.java
    @Bean
    @ConditionalOnBean(InvalidationBus.class)
    @ConditionalOnProperty(value = "ingot.dict.client.invalidation-enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(DictCacheCoordinator.class)
    public DictCacheCoordinator dictCacheCoordinator(InvalidationBus bus,
                                                     @Qualifier("dictService") DictService dictService) {
        return new DictCacheCoordinator(bus, dictService);
    }
}
```

`DictClientAutoConfiguration` 的装配顺序：

| 顺序 | Bean | 作用 | 注册条件 |
|-----|------|------|---------|
| 1 | `dictDelegate`（`RemoteDictService`） | RPC 远端实现 | 类路径有 `Feign` + 存在 `RemotePmsDictService` + 不存在同名 bean |
| 1' | `dictDelegate`（`LocalDictService`） | 本地实现 | 由 PMS 的 `LocalDictConfig` 提前注册（同名 bean，优先生效） |
| 2 | `RedisDictService` | L2 共享缓存层 | 存在 `StringRedisTemplate` + `redis-enabled=true` |
| 3 | `dictService` `@Primary` | 业务方注入入口 | 存在 `dictDelegate`，按 `cacheEnabled` 决定是否叠加 L1 |
| 4 | `DictCacheCoordinator` | 失效广播订阅器 | 存在 `InvalidationBus` + `invalidation-enabled=true` |

### 自动选择 Local vs Remote 的关键

PMS 自身：在自身 `LocalDictConfig` 中提前注册同名 bean `dictDelegate`（`LocalDictService`），此后 `DictClientAutoConfiguration` 中带 `@ConditionalOnMissingBean(name = "dictDelegate")` 的远端实现自动让位。

其它微服务：没有 `LocalDictConfig`，但通过 `ingot-pms-api` 引入了 `RemotePmsDictService` Feign 接口；此时框架自动注册 `RemoteDictService`，业务方注入到的 `DictService` 就是远端实现。

业务方代码完全不需要感知差异。

---

## 3. 三级缓存设计

### 3.1 各级缓存的作用与权衡

| 层级 | 实现 | 默认 TTL | 作用域 | 命中延迟 | 解决的问题 |
|------|------|---------|-------|----------|-----------|
| L1 | Caffeine（进程内） | 5 min | 单 JVM | < 1 ms | 高频读取的就地复用，零网络开销 |
| L2 | Redis（集群共享） | 30 min | 全集群 | 2–5 ms | 同 dict 在多实例间共享缓存，避免每节点都打 PMS |
| L3 | MySQL | — | 持久化 | 10–30 ms | 源数据 |

L1 与 L2 的取舍：

- **没有 L2 仅有 L1**：每个节点首次访问都要回源，且节点数量越多，PMS 压力越大；多节点缓存相互独立，更新失效后存在短暂不一致窗口。
- **没有 L1 仅有 L2**：每次都要走网络访问 Redis，对超高频字典调用不够极致。
- **L1 + L2 组合**：高频字典几乎全部在 L1 命中；新节点冷启动或 L1 过期后从 L2 拿到一份共享结果，回源压力被 Redis 完全吸收。

### 3.2 装饰器链的失效传播

`evict(dictCode)` 与 `evictAll()` 是装饰器链中唯一会"反向作用"于 delegate 的方法：

```81:95:ingot-framework/ingot-dict-client/src/main/java/com/ingot/framework/dict/client/internal/CaffeineDictService.java
    @Override
    public void evict(String dictCode) {
        if (enabled && dictCode != null) {
            cache.asMap().keySet().removeIf(key -> dictCode.equals(key.code));
        }
        // 沿装饰器链向下传播
        delegate.evict(dictCode);
    }

    @Override
    public void evictAll() {
        if (enabled) {
            cache.invalidateAll();
        }
        delegate.evictAll();
    }
```

调用顺序：`Caffeine.evict → Redis.evict → delegate.evict`，确保 L1 + L2 全部清空。`RedisDictService` 的实现使用 `SCAN MATCH <prefix>:dict:items:<dictCode>:*` + `DEL`，避免阻塞 Redis 主线程：

```168:191:ingot-framework/ingot-dict-client/src/main/java/com/ingot/framework/dict/client/internal/RedisDictService.java
    private void deleteByPattern(String pattern) {
        try {
            Set<String> keys = scanKeys(pattern);
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
                if (log.isDebugEnabled()) {
                    log.debug("[Dict] L2 evict pattern={}, count={}", pattern, keys.size());
                }
            }
        } catch (Exception e) {
            log.warn("[Dict] L2 evict failed pattern={}", pattern, e);
        }
    }

    private Set<String> scanKeys(String pattern) {
        Set<String> result = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(200).build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                result.add(cursor.next());
            }
        }
        return result;
    }
```

### 3.3 Redis Key 设计

```
<keyPrefix>:dict:items:<dictCode>:<scope>:<tenantId>:<appId>:<includeDisabled>
```

例：`in:dict:items:user_status:PLATFORM:_:_:0`

- 默认 `keyPrefix = in`，与 `CacheConstants.IGNORE_TENANT_PREFIX` 一致，意味着字典缓存**不绑定单租户上下文**，跨租户复用同一份；不同租户/应用维度通过 key 后段区分。
- `tenantId` / `appId` 缺省时占位为 `_`，避免与真实 ID 冲突。
- `includeDisabled` 单独标识管理端"含禁用项"的查询变体，与业务读路径互不干扰。

---

## 4. 跨节点失效广播

### 4.1 为什么需要

L1 是进程内缓存，多实例部署时一个节点的写入无法清除其它节点的 L1；即便 L2 被清空，其它节点的 L1 仍会持续返回旧值直到 TTL 自然过期。所以必须有显式的"广播 + 清缓存"机制。

### 4.2 完整时序

```
[节点 A] 管理员调用 PUT /v1/platform/base/dict
   │
   │ (1) PlatformDictServiceImpl.update(...)
   │     └─ updateById(...)  // 数据库写入
   │     └─ applicationEventPublisher.publishEvent(DictChangedSpringEvent.of(this, "user_status"))
   │
   │ (2) Spring 事务提交（COMMIT）
   ▼
[节点 A] DictInvalidationPublisher.@TransactionalEventListener(AFTER_COMMIT)
   │
   │ (3) redisLayer.evict("user_status")
   │     └─ Redis SCAN+DEL: in:dict:items:user_status:*
   │
   │ (4) invalidationBus.publish(DictInvalidationEvent.of("user_status"))
   ▼
[Redis] PUBLISH in:bus:dict.invalidate "{...origin=A,dictCode=user_status...}"
   │
   ├─────────────────────┬─────────────────────┐
   ▼                     ▼                     ▼
[节点 A]             [节点 B]              [节点 C]
RedisInvalidationBus.MessageListener
   │ origin == self?
   ├── A: yes → 跳过      ├── B: no → 处理      ├── C: no → 处理
   ▼                     ▼                     ▼
                    DictCacheCoordinator.handle(event)
                            │
                            ▼
                    dictService.evict("user_status")
                       (Caffeine.evict → Redis.evict → delegate.evict)
```

### 4.3 关键设计决策

#### (a) 为什么要在事务提交后才广播

`@TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)` 保证：

- 事务回滚时**不**广播，避免其它节点错误地删了自己的缓存又从 PMS 重新加载到旧值；
- 即使没有事务上下文（例如脚本/管理任务直接调用），`fallbackExecution = true` 仍会触发广播，避免功能在无事务场景下静默失效。

#### (b) 为什么 origin 节点要"先清 L2 再广播"

```37:51:ingot-service/ingot-pms/ingot-pms-provider/src/main/java/com/ingot/cloud/pms/service/dict/DictInvalidationPublisher.java
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onDictChanged(DictChangedSpringEvent event) {
        // 1) origin 端立即清 L2，避免广播到达前其它节点回填旧值
        RedisDictService redisLayer = redisDictServiceProvider.getIfAvailable();
        if (redisLayer != null) {
            try {
                if (event.isAll()) {
                    redisLayer.evictAll();
                } else {
                    redisLayer.evict(event.getDictCode());
                }
            } catch (Exception e) {
                log.warn("[Dict] origin L2 eager-evict failed dictCode={}", event.getDictCode(), e);
            }
        }
```

考虑这种竞态：

```
T0   节点 A 写库提交
T1   节点 A 准备广播
T2   节点 B 此时正好读字典，L1 miss、L2 命中（旧值）→ 把旧值写回 L1
T3   节点 A 广播失效
T4   节点 B 收到广播清 L1+L2
```

如果在 T1 时刻 origin 没有先清 L2，T2 阶段其它节点很可能从 L2 读到旧值后写回 L1；虽然 T4 又会清掉，但中间有一段窗口期返回了旧值。

`origin 端先清 L2 → 再广播` 把这段窗口压缩到只剩"DB 提交 → origin 清 L2"的极短时间，进一步弱化为最终一致。

#### (c) 为什么 origin 自身也订阅但要回环过滤

`DictCacheCoordinator` 在所有装配了 `dict-client` 的节点上一致启动，因此 origin 节点也会收到自己发布的事件。`RedisInvalidationBus` 在订阅端做 `origin == self ? skip : handle` 过滤：

```83:88:ingot-framework/ingot-event-bus/src/main/java/com/ingot/framework/eventbus/redis/RedisInvalidationBus.java
                if (Objects.equals(origin, event.getOrigin())) {
                    if (log.isTraceEnabled()) {
                        log.trace("[EventBus] skip self channel={}", channel);
                    }
                    return;
                }
                handler.accept(event);
```

origin 节点的 L1 是否也需要清？答案是——**写 PMS 的请求也是经过 `DictService` 装饰器链的吗？不是**。`PlatformDictServiceImpl` 直接走 MyBatis，并不会主动 evict L1；所以严格地说 origin 自身的 L1 仍需要显式清除。当前实现选择用一个独立的本地 Spring listener（如有需要可补充）或依赖 L1 短 TTL（默认 5 min）兜底。

> **设计取舍**：当前方案不要求"原节点写完立刻读到新值"——管理员页面在写后通常会重新拉列表，此时新值会从 L2 / DB 命中再回填。若有强一致需求，可在 `DictInvalidationPublisher` 里追加一行 `dictService.evict(...)`（直接走 origin 自身的装饰器链）。

#### (d) 为什么把广播抽到 `ingot-event-bus`

字典只是众多需要"集群级缓存失效"的场景之一（社交账号绑定、权限缓存、配置中心等都有相同需求）。为避免每个领域都重新发明一遍 channel + JSON + origin 过滤，将这套机制抽象为：

- `InvalidationBus` SPI（与中间件无关）
- `InvalidationEvent` 基类（统一 origin / timestamp）
- `@EventType` 注解（声明式 channel 命名）

这样：

1. **新场景接入只需写 `@EventType + 一个事件类 + 订阅器`**；
2. **替换中间件（Kafka / RocketMQ）只需替换 `InvalidationBus` 的实现**，业务零改动；
3. **社交配置（`ingot-social-common`）已迁移至同一总线**：`SocialInvalidationEvent`（`@EventType("social.invalidate")`）由 `InvalidationBusSocialConfigMessagePublisher` 发布，`SocialInvalidationCoordinator` 订阅；详见 `ingot-social-common/README.md`。

### 4.4 channel 命名规则

```
<topicPrefix>:<EventType.value>
```

| 事件类 | `@EventType` | 实际 channel（默认 prefix `in:bus`） |
|--------|--------------|----------------------------------------|
| `DictInvalidationEvent` | `dict.invalidate` | `in:bus:dict.invalidate` |
| `SocialInvalidationEvent` | `social.invalidate` | `in:bus:social.invalidate` |
| 未来 `PermissionMatrixChangedEvent`（示例） | `permission.matrix.refresh` | `in:bus:permission.matrix.refresh` |

命名约定：`<domain>.<action>[.<subaction>]`，全小写、点分隔；同 domain 内类型唯一。

---

## 5. 失效粒度

| 触发动作 | 事件粒度 | 受影响缓存 |
|---------|---------|-----------|
| `create(PlatformDict)` | 单个 `dictCode` | `in:dict:items:<dictCode>:*` |
| `update(PlatformDict)` | 单个 `dictCode`（取更新前的 code） | 同上 |
| `delete(id)` | 单个 `dictCode` | 同上 |
| `changeStatus(id, status)` | 单个 `dictCode` | 同上 |
| `batchSort(items)` | 全量 (`all=true`) | `in:dict:items:*` |

`batchSort` 涉及多个不同字典编码的 sort 调整，逐个拆分广播代价高且容易遗漏，因此选择简单可靠的"全量清"——这是企业管理场景下可以接受的折中（排序操作低频、清空后下次访问立即从 DB 重建）。

---

## 6. 容错与降级

| 故障 | 影响 | 行为 |
|-----|------|-----|
| Redis 宕机 | L2 不可用 | `RedisDictService` 所有读写都 try-catch，失败时 fallback 到 delegate（PMS Local 或 Feign），日志 WARN |
| `RedisInvalidationBus.publish` 失败 | 此次写入未广播 | log WARN；其它节点等 L1 自然过期（默认 5 min）后回源恢复一致 |
| Feign 调用 PMS 失败 | 远端 delegate 拿不到数据 | `RemoteDictService` 返回 `List.of()`，业务方收到空列表（视上下文使用 `label()` 的 fallback：返回原始 value） |
| ObjectMapper 反序列化失败 | 单条 L2 数据格式异常 | log WARN，当作 L2 miss，回源后用新值覆盖 |
| 事件总线被禁用 (`ingot.event-bus.type=none`) | 没有跨节点失效 | `DictCacheCoordinator` 因缺 `InvalidationBus` 不注册；其它节点依赖 L1 TTL 收敛 |

**核心思想**：所有缓存层都是"加速路径"，任何一层故障都允许直接 fallback 到下一层乃至源数据库，不会因缓存中间件挂掉而拖垮业务读路径。

---

## 7. 与其它方案的对比

| 方案 | 一致性 | 写开销 | 运维复杂度 | 备注 |
|-----|-------|-------|-----------|------|
| 仅 L1 + 短 TTL | 弱（最长 TTL 不一致） | 低 | 低 | 简单但不适合管理端要求"立即生效"的场景 |
| L1 + Redis L2 + TTL | 弱 | 低 | 中 | 节省 PMS 压力但仍有不一致窗口 |
| **L1 + L2 + 失效广播（本方案）** | 最终一致（< 50 ms） | 低（一次 evict + 一次 PUBLISH） | 中 | 当前选型 |
| L1 + L2 + 强一致（分布式锁/版本号） | 强 | 高（每读取 GET 校验版本） | 高 | 字典读多写极少，性价比不足 |

---

## 8. 配置项总览

完整配置示例与默认值见 [API / RPC 参考 §配置项](./API-REFERENCE.md#配置项)。常用项：

| 配置 | 默认值 | 说明 |
|-----|-------|------|
| `ingot.dict.client.mode` | `AUTO` | 自动选 Local/Remote |
| `ingot.dict.client.cache-enabled` | `true` | L1 开关 |
| `ingot.dict.client.cache-ttl` | `5m` | L1 TTL |
| `ingot.dict.client.redis-enabled` | `true` | L2 开关 |
| `ingot.dict.client.redis-ttl` | `30m` | L2 TTL |
| `ingot.dict.client.redis-key-prefix` | `in` | L2 key 前缀 |
| `ingot.dict.client.invalidation-enabled` | `true` | 是否订阅失效广播 |
| `ingot.event-bus.type` | `redis` | 总线实现，`none` 关闭 |
| `ingot.event-bus.origin` | `${spring.application.name}:UUID` | 节点标识，留空自动生成 |
| `ingot.event-bus.redis.topic-prefix` | `in:bus` | channel 名前缀 |

---

## 9. 扩展点

### 9.1 自定义 `InvalidationBus`（例如 Kafka 实现）

```java
@Component
@ConditionalOnProperty(value = "ingot.event-bus.type", havingValue = "kafka")
public class KafkaInvalidationBus implements InvalidationBus {

    @Override
    public <E extends InvalidationEvent> void publish(E event) {
        String topic = EventTypeResolver.channel("ingot.bus", event.getClass());
        // ... kafka producer send
    }

    @Override
    public <E extends InvalidationEvent> Subscription subscribe(Class<E> eventType, Consumer<E> handler) {
        // ... kafka consumer subscribe
    }
}
```

由于 `EventBusAutoConfiguration` 注册的 `RedisInvalidationBus` 带 `@ConditionalOnMissingBean(InvalidationBus.class)`，自定义实现会自动接管。

### 9.2 自定义事件类型

```java
@EventType("permission.matrix.refresh")
public class PermissionMatrixChangedEvent extends InvalidationEvent {
    private Long roleId;
    // getter / setter / @JsonCreator
}
```

订阅端：

```java
@Component
@RequiredArgsConstructor
public class PermissionCacheCoordinator {
    private final InvalidationBus bus;
    private final PermissionCache cache;
    private Subscription subscription;

    @PostConstruct
    void start() {
        subscription = bus.subscribe(PermissionMatrixChangedEvent.class, e -> cache.evict(e.getRoleId()));
    }

    @PreDestroy
    void stop() { if (subscription != null) subscription.close(); }
}
```

### 9.3 替换 `dictDelegate` 实现

通过提供同名 `dictDelegate` bean 即可短路自动配置：

```java
@Bean(name = "dictDelegate")
public DictService dictDelegate() {
    return new MyCustomDictService();
}
```

适用场景：测试、单元基准、定制化字典源（例如配置中心、Apollo）。

---

## 10. 已知边界与注意事项

1. **L1 ≠ 强一致**：origin 节点写后到自身 L1 自然过期之间，仍可能短暂返回旧值（见 §4.3 (c)）；如果业务对写后立即读敏感，请在写路径补一行 `dictService.evict(code)`。
2. **`SCAN` 在大 keyspace 下有开销**：`RedisDictService` 用 `SCAN ... COUNT 200` 而非 `KEYS`，但极端情况下若字典数量巨大（万级以上），建议进一步分桶或改为基于元数据集合的精确删除。
3. **JSON 序列化兼容性**：`DictItem.extra` 使用 `Map<String, Object>`，跨版本反序列化时若新增字段需保证向后兼容（不要使用对象类型而是 Map）。
4. **节点 `origin` 规则**：默认 `${spring.application.name}:UUID`，每次重启变化。如需稳定 origin（例如运维侧调试），可显式配置 `ingot.event-bus.origin`。
5. **Redis Pub/Sub 不持久化**：节点宕机期间错过的失效事件不会重放；恢复后依赖 L1/L2 TTL 自然收敛。如需可靠投递可替换为 Kafka 实现。
6. **`RedisMessageListenerContainer` 全应用唯一**：框架 `InRedisMessageConfiguration#redisContainer` 提供唯一一个 `@ConditionalOnMissingBean` 容器。`ingot-event-bus` 的 `RedisInvalidationBus`、以及任何自定义 `InvalidationBus` 的 Redis 实现，均应**注入并复用**该容器注册订阅，**不要自建第二个容器**——否则会触发「required a single bean, but 2 were found」类启动错误。社交配置跨节点失效已走 `InvalidationBus`（`SocialInvalidationEvent`），不再使用独立 `in:social:config:changed` 频道或单独向容器注册 legacy social 监听器。
7. **自动配置顺序与跨节点失效**：`EventBusAutoConfiguration` 通过 `@ConditionalOnBean(RedisMessageListenerContainer.class)` 注册 `InvalidationBus`。该类**必须**排在 `ingot-data-redis` 的 `InRedisTemplateConfiguration` / `InRedisMessageConfiguration` **之后**执行；若仅靠 `@AutoConfigureAfter(RedisAutoConfiguration.class)`，在拓扑排序下仍可能早于上述 Configuration，导致总线 bean 整条链路静默跳过——表现恰为「PMS 写端本地缓存正常失效，其它服务（如 AUTH）L1 永远不过期」。已在 `ingot-event-bus` 中用 `@AutoConfigureAfter(..., name = { InRedisTemplateConfiguration, InRedisMessageConfiguration })` 显式约束。启动后应在**每个消费字典的微服务**日志中看到：`[EventBus] initialized RedisInvalidationBus`、`[EventBus] subscribed channel=...dict.invalidate`、`[Dict] cache coordinator subscribed`；缺任何一条都说明订阅链路未装配。

---

下一步阅读：[使用指南](./USAGE.md) | [API / RPC 参考](./API-REFERENCE.md)
