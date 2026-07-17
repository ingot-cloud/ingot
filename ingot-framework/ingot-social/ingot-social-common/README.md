# Ingot Social Common - 社交集成公共模块

## 概述

`ingot-social-common` 提供社交配置在**多实例间同步刷新**的通用能力：

- 统一的配置属性（`SocialConfigProperties`）
- 配置变更的**进程内**事件：`SocialConfigChangedEvent`（按 `SocialTypeEnum` 区分平台）
- **跨节点**失效广播：通过 `ingot-event-bus` 的 `InvalidationBus` 发布 `SocialInvalidationEvent`
- 默认装配：`InvalidationBusSocialConfigMessagePublisher` + `SocialInvalidationCoordinator`

业务侧（如微信小程序模块）只需监听 `SocialConfigChangedEvent`，并**按社交类型过滤**，无需关心传输层。

## 跨节点机制（与字典对齐）

### 频道命名

与字典相同，由 `EventTypeResolver` 推导：

```text
channel = <ingot.event-bus.redis.topic-prefix> + ":" + <@EventType>
```

| 事件类 | `@EventType` | 默认 channel（`topic-prefix = in:bus`） |
|--------|--------------|----------------------------------------|
| `SocialInvalidationEvent` | `social.invalidate` | `in:bus:social.invalidate` |

各微服务 **必须使用相同的 `ingot.event-bus.redis.topic-prefix`**，否则收不到对端广播。

### 发布与订阅

1. **发布端**（如 PMS 的 `SysSocialDetailsServiceImpl` 调 `SocialConfigMessagePublisher`）  
   - 先通过 `SocialConfigMessageHandler.handleInvalidation` 在本进程发布 `SocialConfigChangedEvent`（**必须**：`RedisInvalidationBus` 会过滤 **origin 回环**，本机不能只依赖订阅）。  
   - 再 `InvalidationBus.publish(SocialInvalidationEvent)`，其它实例收到后同样转为 `SocialConfigChangedEvent`。

2. **订阅端**  
   - `SocialInvalidationCoordinator` 在启动时 `bus.subscribe(SocialInvalidationEvent.class, ...)`，与 `DictCacheCoordinator` 类似，仅处理**来自其它节点**的消息。

### 载荷格式

`SocialInvalidationEvent` 的 JSON 载荷与历史 `SocialConfigRedisMessage` 字段一致，便于自定义实现继续复用同一套 DTO：

```json
{
  "socialType": "WECHAT_MINI_PROGRAM",
  "changeType": "UPDATE",
  "appId": "wx123456",
  "origin": "ingot-pms:…",
  "timestamp": 1715587200000
}
```

（`origin` / `timestamp` 由总线在发布侧补齐。）

## 核心特性：按社交类型通知

所有事件都带 `socialType`，各平台模块只处理自己的枚举值即可：

```java
@EventListener
public void onConfigChanged(SocialConfigChangedEvent event) {
    if (event.getSocialType() != SocialTypeEnum.WECHAT_MINI_PROGRAM) {
        return;
    }
    //  refresh 微信侧 WxMaConfigManager …
}
```

## 依赖

```gradle
dependencies {
    implementation project(':ingot-framework:ingot-social-common')
}
```

`ingot-social-common` **传递依赖** `ingot-event-bus`（API）。实际运行还需要：

- 各服务已引入 **`ingot-data-redis`**（或等价装配），以注册 `StringRedisTemplate`、统一的 `RedisMessageListenerContainer` 及 **`InvalidationBus`**（见 `EventBusAutoConfiguration`）。
- 若 classpath 上始终没有 `InvalidationBus`，默认发布器仍会**本机**发出 `SocialConfigChangedEvent`，但**不会做跨节点广播**（`ObjectProvider<InvalidationBus>` 为空）。

## 配置

### 必配（与字典、其它失效广播共用）

```yaml
ingot:
  event-bus:
    redis:
      topic-prefix: in:bus   # 默认值；全集群一致
```

### 已废弃（保留字段仅为兼容旧 YAML，逻辑不读）

```yaml
ingot:
  social:
    redis:
      topic: in:social:config:changed   # 已废弃，请改用上表 event-bus.topic-prefix
```

## 核心组件

### `SocialConfigChangedEvent`

进程内 Spring 事件：`socialType`、`changeType`（`ADD` / `UPDATE` / `DELETE` / `REFRESH_ALL`）、`appId`。

### `SocialInvalidationEvent`

跨节点 `InvalidationEvent` 子类，定义见上文频道表。

### `SocialConfigMessagePublisher`

```java
void publishRefreshAll(SocialTypeEnum socialType);
void publishAdd(SocialTypeEnum socialType, String appId);
void publishUpdate(SocialTypeEnum socialType, String appId);
void publishDelete(SocialTypeEnum socialType, String appId);
void publish(SocialConfigRedisMessage message);
```

**默认实现**：`InvalidationBusSocialConfigMessagePublisher`（无自定义 Bean 时由 `SocialCommonConfiguration` 注册）。

### `SocialCommonConfiguration`

- `@AutoConfiguration`，且 `@AutoConfigureAfter(EventBusAutoConfiguration.class)`，避免早于总线装配。

### 消息流转（简图）

```text
PMS 写库 → SysSocialDetailsServiceImpl
    → SocialConfigMessagePublisher
        → 本机 SocialConfigChangedEvent
        → InvalidationBus.publish(SocialInvalidationEvent)
                ↓ Redis Pub/Sub（in:bus:social.invalidate）
其它实例 SocialInvalidationCoordinator → handleInvalidation → SocialConfigChangedEvent
    → 各平台 @EventListener（按 socialType 过滤）
```

## 扩展：自定义传输（Kafka 等）

若需用 Kafka 等替代 Redis Pub/Sub，可自写 `SocialConfigMessagePublisher`，在消费端把反序列化结果交给 `SocialConfigMessageHandler.handleMessage(json, source)`。详见本目录 [EXTENSION_GUIDE.md](./EXTENSION_GUIDE.md)。

## 相关文档

- [EXTENSION_GUIDE.md](./EXTENSION_GUIDE.md) — 自定义发布器与监听器
- [ingot-social-wechat/README.md](../ingot-social-wechat/README.md) — 微信实现
- 字典侧总线说明：[docs/modules/dict/ARCHITECTURE.md](../../../docs/modules/dict/ARCHITECTURE.md)（§4、channel 命名）

---

**作者**：jy  
**最近更新**：2026-05
