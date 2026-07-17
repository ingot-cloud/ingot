# 社交模块（Social）文档

社交配置多实例热更新已与 **`ingot-event-bus` / `InvalidationBus`** 对齐，与字典域共用 Redis Pub/Sub channel 命名规则（`{topic-prefix}:<EventType>`）。

## 模块文档（源码树）

| 文档 | 说明 |
|------|------|
| [ingot-social-common/README.md](../../ingot-framework/ingot-social/ingot-social-common/README.md) | 公共模块、默认发布器与协调器、`SocialInvalidationEvent` |
| [ingot-social-common/EXTENSION_GUIDE.md](../../ingot-framework/ingot-social/ingot-social-common/EXTENSION_GUIDE.md) | 自定义 `SocialConfigMessagePublisher`（Kafka 等） |
| [ingot-social-wechat/README.md](../../ingot-framework/ingot-social/ingot-social-wechat/README.md) | 微信小程序集成 |

## 与字典文档的关系

失效总线通用设计、channel 表、`RedisMessageListenerContainer` 约束等见 [dict/ARCHITECTURE.md](../dict/ARCHITECTURE.md)。

---

**维护**：与 `ingot-social-common` 代码同步更新。
