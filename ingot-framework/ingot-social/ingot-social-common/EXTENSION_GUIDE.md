# Social 模块扩展指南

## 设计理念

`ingot-social-common` 采用 **接口抽象 + 默认实现**：

- **接口**：`SocialConfigMessagePublisher`
- **默认实现**：`InvalidationBusSocialConfigMessagePublisher` — 本机 `SocialConfigChangedEvent` + `InvalidationBus.publish(SocialInvalidationEvent)`，与 `ingot-dict-client` 的跨节点模式一致。
- **扩展**：自定义 `@Bean SocialConfigMessagePublisher` 可覆盖默认实现（`@ConditionalOnMissingBean`）。

## 默认实现：InvalidationBus（推荐）

### 行为摘要

1. 发布时先 `SocialConfigMessageHandler.handleInvalidation`，再 `InvalidationBus.publish`（**本机必须先走本地事件**：总线订阅端会忽略 `origin` 相同的回环消息）。
2. 其它节点由 `SocialInvalidationCoordinator` 订阅 `SocialInvalidationEvent`（channel：`{topic-prefix}:social.invalidate`）。
3. 各业务模块继续只监听 `SocialConfigChangedEvent`，按 `socialType` 过滤即可。

### 配置

与全局失效总线共用前缀：

```yaml
ingot:
  event-bus:
    redis:
      topic-prefix: in:bus   # 全集群一致
```

`ingot.social.redis.topic` **已废弃**，勿再依赖独立 Redis 频道。

### 运维自检日志

- `[EventBus] subscribed channel=...social.invalidate`
- `[Social] invalidation coordinator subscribed`

## 自定义实现：Kafka / MQ（可选）

当项目已统一使用 Kafka 等，且希望社交配置走独立 Topic 时，可提供自有 `SocialConfigMessagePublisher`。请注意：

1. **接口需实现全部方法**：`publishRefreshAll`、`publishAdd`、`publishUpdate`、`publishDelete`、`publish(SocialConfigRedisMessage)`。
2. **消费侧**应把载荷 JSON 交给 `SocialConfigMessageHandler.handleMessage(String json, Object source)`，以复用「解析 → `SocialConfigChangedEvent`」逻辑。
3. **自定义发布器时**：框架**不会**再注册默认的 `InvalidationBusSocialConfigMessagePublisher`，但 **`SocialInvalidationCoordinator` 仍可能随 `InvalidationBus` 存在而订阅 `social.invalidate`**。若你的实现**完全不**往该 channel 发消息，该订阅仅为空转，无副作用；若希望避免混淆，可在业务侧文档中说明「社交配置以 Kafka 为准，Redis 总线仅用于其它域」。

### 示例：Kafka 发布器（节选）

实现时注意与 **`DELETE`** 命名一致（接口为 `publishDelete`，消息工厂为 `SocialConfigRedisMessage.delete`）。

```java
@Slf4j
@RequiredArgsConstructor
public class KafkaSocialConfigMessagePublisher implements SocialConfigMessagePublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    @Override
    public void publishRefreshAll(SocialTypeEnum socialType) {
        send(SocialConfigRedisMessage.refreshAll(socialType.getValue()));
    }

    @Override
    public void publishAdd(SocialTypeEnum socialType, String appId) {
        send(SocialConfigRedisMessage.add(socialType.getValue(), appId));
    }

    @Override
    public void publishUpdate(SocialTypeEnum socialType, String appId) {
        send(SocialConfigRedisMessage.update(socialType.getValue(), appId));
    }

    @Override
    public void publishDelete(SocialTypeEnum socialType, String appId) {
        send(SocialConfigRedisMessage.delete(socialType.getValue(), appId));
    }

    @Override
    public void publish(SocialConfigRedisMessage message) {
        send(message);
    }

    private void send(SocialConfigRedisMessage message) {
        try {
            kafkaTemplate.send(topic, objectMapper.writeValueAsString(message));
        } catch (Exception e) {
            log.error("Kafka social config publish failed", e);
        }
    }
}
```

### 示例：Kafka 监听（复用 Handler）

```java
@Slf4j
@RequiredArgsConstructor
public class KafkaSocialConfigMessageListener {

    private final SocialConfigMessageHandler messageHandler;

    @KafkaListener(topics = "${ingot.social.kafka.topic}", groupId = "${spring.application.name}")
    public void onMessage(String message) {
        messageHandler.handleMessage(message, this);
    }
}
```

注册自定义 Bean 时提供 `SocialConfigMessagePublisher`（及监听器）；框架默认 Redis 总线实现会被 `@ConditionalOnMissingBean` 跳过。

如需 **同时在 Kafka 与 InvalidationBus 上双写**，可自行组合调用，一般不推荐以免重复刷新。

## 职责划分

```text
框架（ingot-social-common）
  ├─ SocialConfigMessagePublisher（接口）
  ├─ SocialInvalidationEvent + SocialInvalidationCoordinator（默认跨节点）
  ├─ SocialConfigMessageHandler（JSON → SocialConfigChangedEvent）
  └─ SocialConfigRedisMessage（载荷 DTO，兼容历史命名）

业务服务
  ├─ 默认：引入 data-redis + event-bus，使用 InvalidationBus
  └─ 或：自定义 Publisher + MQ Listener（handleMessage）
```

## 接口说明

### SocialConfigMessagePublisher

```java
public interface SocialConfigMessagePublisher {
    void publishRefreshAll(SocialTypeEnum socialType);
    void publishAdd(SocialTypeEnum socialType, String appId);
    void publishUpdate(SocialTypeEnum socialType, String appId);
    void publishDelete(SocialTypeEnum socialType, String appId);
    void publish(SocialConfigRedisMessage message);
}
```

### SocialConfigMessageHandler

- `handleMessage(String messageBody, Object source)` — 解析 JSON（`SocialConfigRedisMessage` 形态）并发布 `SocialConfigChangedEvent`。
- `handleInvalidation(SocialInvalidationEvent invalidation, Object source)` — 由默认发布器与协调器在总线路径上调用。

### SocialConfigRedisMessage

通用载荷（名称保留历史约定）：`socialType`、`changeType`、`appId`、`timestamp`；静态工厂 `refreshAll` / `add` / `update` / `delete`。

---

**版本**：v2.0（InvalidationBus 迁移后）  
**作者**：jy  
**最近更新**：2026-05
