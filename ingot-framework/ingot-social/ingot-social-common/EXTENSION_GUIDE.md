# Social 模块扩展指南

## 🎯 设计理念

`ingot-social-common` 模块遵循 **接口抽象 + 默认实现** 的设计原则：

```
框架提供：
  ├─ 核心接口（SocialConfigMessagePublisher）
  ├─ 默认实现（Redis - 简单、常用）
  └─ 扩展机制（@ConditionalOnMissingBean）

服务扩展：
  └─ 根据需要自定义实现（Kafka / RabbitMQ / RocketMQ 等）
```

## 📦 默认实现：Redis

### 为什么选择 Redis 作为默认实现？

✅ **配置简单**：只需要 Redis 连接信息，无需额外配置  
✅ **广泛使用**：大多数项目已经使用 Redis  
✅ **轻量级**：Pub/Sub 机制简单高效  
✅ **零维护**：无需管理 Topic、Partition 等  

### 默认配置

```yaml
# application.yml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: your-password

# 可选：自定义 Topic
ingot:
  social:
    redis:
      topic: in:social:config:changed  # 默认值
```

## 🔧 自定义实现

### 场景：使用 Kafka

如果你的服务已经使用 Kafka，并且希望统一使用 Kafka 作为消息队列：

#### 1️⃣ 添加 Kafka 依赖

```gradle
// build.gradle
dependencies {
    implementation project(ingot.framework_social_wechat)
    
    // 添加 Kafka 依赖
    implementation("org.springframework.kafka:spring-kafka")
}
```

#### 2️⃣ 配置 Kafka

```yaml
# application.yml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
    consumer:
      group-id: ${spring.application.name}
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer

ingot:
  social:
    kafka:
      topic: in-social-config-changed
```

#### 3️⃣ 实现消息发布器

```java
package com.yourcompany.service.social.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.social.common.event.SocialConfigRedisMessage;
import com.ingot.framework.social.common.publisher.SocialConfigMessagePublisher;
import com.ingot.framework.commons.model.enums.SocialTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Kafka 消息发布器实现
 */
@Slf4j
@RequiredArgsConstructor
public class KafkaSocialConfigMessagePublisher implements SocialConfigMessagePublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    @Override
    public void publishUpdate(SocialTypeEnum socialType, String appId) {
        try {
            SocialConfigRedisMessage message = SocialConfigRedisMessage.update(
                    socialType.getValue(), appId);
            String messageJson = objectMapper.writeValueAsString(message);
            
            kafkaTemplate.send(topic, messageJson);
            log.info("KafkaPublisher - 发送更新消息: type={}, appId={}", socialType, appId);
        } catch (Exception e) {
            log.error("KafkaPublisher - 发送更新消息失败", e);
        }
    }

    @Override
    public void publishRemove(SocialTypeEnum socialType, String appId) {
        try {
            SocialConfigRedisMessage message = SocialConfigRedisMessage.remove(
                    socialType.getValue(), appId);
            String messageJson = objectMapper.writeValueAsString(message);
            
            kafkaTemplate.send(topic, messageJson);
            log.info("KafkaPublisher - 发送删除消息: type={}, appId={}", socialType, appId);
        } catch (Exception e) {
            log.error("KafkaPublisher - 发送删除消息失败", e);
        }
    }

    @Override
    public void publishRefreshAll(SocialTypeEnum socialType) {
        try {
            SocialConfigRedisMessage message = SocialConfigRedisMessage.refreshAll(
                    socialType.getValue());
            String messageJson = objectMapper.writeValueAsString(message);
            
            kafkaTemplate.send(topic, messageJson);
            log.info("KafkaPublisher - 发送刷新消息: type={}", socialType);
        } catch (Exception e) {
            log.error("KafkaPublisher - 发送刷新消息失败", e);
        }
    }
}
```

#### 4️⃣ 实现消息监听器

```java
package com.yourcompany.service.social.kafka;

import com.ingot.framework.social.common.event.SocialConfigMessageHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;

/**
 * Kafka 消息监听器
 */
@Slf4j
@RequiredArgsConstructor
public class KafkaSocialConfigMessageListener {

    private final SocialConfigMessageHandler messageHandler;

    @KafkaListener(
        topics = "${ingot.social.kafka.topic}",
        groupId = "${spring.application.name}"
    )
    public void onMessage(String message) {
        log.debug("KafkaListener - 接收到消息: {}", message);
        messageHandler.handleMessage(message, this);
    }
}
```

#### 5️⃣ 注册自定义实现

```java
package com.yourcompany.service.social.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.social.common.event.SocialConfigMessageHandler;
import com.ingot.framework.social.common.properties.SocialConfigProperties;
import com.ingot.framework.social.common.publisher.SocialConfigMessagePublisher;
import com.yourcompany.service.social.kafka.KafkaSocialConfigMessageListener;
import com.yourcompany.service.social.kafka.KafkaSocialConfigMessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * 社交配置 - Kafka 实现
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class SocialKafkaConfiguration {

    private final SocialConfigProperties socialConfigProperties;

    /**
     * Kafka 消息发布器
     * 注意：此 Bean 会覆盖默认的 Redis 实现（因为 @ConditionalOnMissingBean）
     */
    @Bean
    public SocialConfigMessagePublisher kafkaSocialConfigMessagePublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper) {
        String topic = socialConfigProperties.getKafka().getTopic();
        log.info("SocialKafkaConfiguration - 初始化Kafka消息发布器，主题: {}", topic);
        return new KafkaSocialConfigMessagePublisher(kafkaTemplate, objectMapper, topic);
    }

    /**
     * Kafka 消息监听器
     */
    @Bean
    public KafkaSocialConfigMessageListener kafkaSocialConfigMessageListener(
            SocialConfigMessageHandler messageHandler) {
        log.info("SocialKafkaConfiguration - 初始化Kafka消息监听器");
        return new KafkaSocialConfigMessageListener(messageHandler);
    }
}
```

### 关键点

1. **`@ConditionalOnMissingBean`**：框架使用此注解，如果服务自定义了 `SocialConfigMessagePublisher`，则不会创建默认的 Redis 实现。

2. **复用 `SocialConfigMessageHandler`**：消息处理逻辑已经封装在此类中，自定义实现只需要调用它。

3. **复用消息格式**：使用 `SocialConfigRedisMessage` 作为消息格式（虽然名字叫 Redis，但实际上是通用的）。

## 🌟 其他消息队列

### RabbitMQ 示例

```java
@Slf4j
@RequiredArgsConstructor
public class RabbitMQSocialConfigMessagePublisher implements SocialConfigMessagePublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final String exchange;
    private final String routingKey;

    @Override
    public void publishUpdate(SocialTypeEnum socialType, String appId) {
        try {
            SocialConfigRedisMessage message = SocialConfigRedisMessage.update(
                    socialType.getValue(), appId);
            String messageJson = objectMapper.writeValueAsString(message);
            
            rabbitTemplate.convertAndSend(exchange, routingKey, messageJson);
            log.info("RabbitMQPublisher - 发送更新消息: type={}, appId={}", socialType, appId);
        } catch (Exception e) {
            log.error("RabbitMQPublisher - 发送更新消息失败", e);
        }
    }
    
    // ... 其他方法
}
```

### RocketMQ 示例

```java
@Slf4j
@RequiredArgsConstructor
public class RocketMQSocialConfigMessagePublisher implements SocialConfigMessagePublisher {

    private final RocketMQTemplate rocketMQTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    @Override
    public void publishUpdate(SocialTypeEnum socialType, String appId) {
        try {
            SocialConfigRedisMessage message = SocialConfigRedisMessage.update(
                    socialType.getValue(), appId);
            String messageJson = objectMapper.writeValueAsString(message);
            
            rocketMQTemplate.convertAndSend(topic, messageJson);
            log.info("RocketMQPublisher - 发送更新消息: type={}, appId={}", socialType, appId);
        } catch (Exception e) {
            log.error("RocketMQPublisher - 发送更新消息失败", e);
        }
    }
    
    // ... 其他方法
}
```

## 职责划分

```
┌─────────────────────────────────────────────────────────────┐
│ 框架层（ingot-social-common）                                │
│   ├─ 定义接口（SocialConfigMessagePublisher）               │
│   ├─ 定义消息格式（SocialConfigRedisMessage）               │
│   ├─ 提供消息处理器（SocialConfigMessageHandler）           │
│   └─ 提供默认实现（Redis - 简单常用）                       │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ 服务层（ingot-pms-provider / other-service）                │
│   ├─ 使用默认实现（Redis）✅                                │
│   └─ 或自定义实现（Kafka / RabbitMQ / RocketMQ）           │
│      ├─ 实现 SocialConfigMessagePublisher                   │
│      ├─ 实现消息监听器                                       │
│      └─ 注册为 Bean（自动覆盖默认实现）                      │
└─────────────────────────────────────────────────────────────┘
```

## 🎯 最佳实践

### 1. 什么时候使用默认实现（Redis）？

✅ 项目已经使用 Redis  
✅ 不需要复杂的消息队列功能  
✅ 追求简单和快速开发  

### 2. 什么时候自定义实现？

✅ 项目已经统一使用某个消息队列（如 Kafka）  
✅ 需要消息持久化、高可靠性  
✅ 需要与现有消息队列基础设施集成  
✅ 需要特定的消息队列特性（如 Kafka 的分区、RabbitMQ 的路由）  

### 3. 如何选择？

```
决策树：
├─ 项目已使用 Kafka/RabbitMQ 等？
│  ├─ 是 → 自定义实现，统一使用该消息队列
│  └─ 否 → 使用默认 Redis 实现
│
└─ 需要高级消息队列特性？
   ├─ 是 → 自定义实现
   └─ 否 → 使用默认 Redis 实现
```

## 📝 接口说明

### SocialConfigMessagePublisher

```java
public interface SocialConfigMessagePublisher {
    
    /**
     * 发布配置更新消息
     * 
     * @param socialType 社交类型（如：微信小程序、QQ等）
     * @param appId 应用ID
     */
    void publishUpdate(SocialTypeEnum socialType, String appId);
    
    /**
     * 发布配置删除消息
     * 
     * @param socialType 社交类型
     * @param appId 应用ID
     */
    void publishRemove(SocialTypeEnum socialType, String appId);
    
    /**
     * 发布全量刷新消息
     * 
     * @param socialType 社交类型
     */
    void publishRefreshAll(SocialTypeEnum socialType);
}
```

### SocialConfigMessageHandler

框架已提供的消息处理器，自定义监听器可以直接使用：

```java
@RequiredArgsConstructor
public class SocialConfigMessageHandler {
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    /**
     * 处理消息（反序列化 + 发布本地事件）
     * 
     * @param message JSON 格式的消息
     * @param source 事件源
     */
    public void handleMessage(String message, Object source);
}
```

### SocialConfigRedisMessage

通用消息格式（虽然名字叫 Redis，但可用于任何消息队列）：

```java
@Data
public class SocialConfigRedisMessage {
    private String changeType;     // UPDATE / REMOVE / REFRESH_ALL
    private String socialType;     // WECHAT_MINI_PROGRAM / QQ 等
    private String appId;          // 应用ID（REFRESH_ALL 时为空）
    
    // 工厂方法
    public static SocialConfigRedisMessage update(String socialType, String appId);
    public static SocialConfigRedisMessage remove(String socialType, String appId);
    public static SocialConfigRedisMessage refreshAll(String socialType);
}
```

## 🎉 总结

### 设计原则

```
简单优先：默认使用 Redis（配置简单、广泛使用）
接口抽象：定义标准接口，支持多种实现
按需扩展：服务根据需要自定义实现
职责清晰：框架提供能力，服务选择方案
```

### 扩展步骤

```
1. 添加消息队列依赖（如 spring-kafka）
2. 配置消息队列（如 bootstrap-servers）
3. 实现 SocialConfigMessagePublisher 接口
4. 实现消息监听器（复用 SocialConfigMessageHandler）
5. 注册为 Bean（自动覆盖默认实现）
```

### 关键点

✅ **自动覆盖**：框架使用 `@ConditionalOnMissingBean`，服务自定义会覆盖默认实现  
✅ **消息复用**：使用统一的消息格式和处理器  
✅ **零侵入**：不使用时，默认实现自动激活  
✅ **完全控制**：自定义时，完全控制配置和实现  

---

**版本**：v1.0  
**作者**：JY & Claude  
**日期**：2025-12-07  
**许可**：MIT

