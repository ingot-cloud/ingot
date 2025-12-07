# Ingot Social Common - 社交集成公共模块

## 概述

`ingot-social-common` 是社交集成的公共基础模块，提供了社交配置管理的通用功能，包括：

- 统一的配置属性管理
- 配置变更通知机制（按社交类型）
- 多种消息队列支持（Redis/Kafka）
- 自动条件装配

## 核心特性

### 1. 按社交类型通知

所有消息和事件都包含 `socialType` 字段，各社交模块可以选择性监听：

```java
// 事件包含社交类型
SocialConfigChangedEvent {
    socialType: SocialTypeEnum.WECHAT_MINI_PROGRAM,  // 微信小程序
    changeType: ADD,
    appId: "wx123456"
}

// 微信模块只监听微信类型
@EventListener
public void onConfigChanged(SocialConfigChangedEvent event) {
    if (event.getSocialType() != SocialTypeEnum.WECHAT_MINI_PROGRAM) {
        return;  // 忽略其他类型
    }
    // 处理微信配置...
}
```

### 2. 多种消息队列支持

- **Redis Pub/Sub**（默认）- 轻量级，低延迟
- **Kafka** - 高吞吐，持久化消息

自动检测依赖并启用：
- 检测到 `spring-data-redis` → 启用Redis
- 检测到 `spring-kafka` 且配置 `message-queue: kafka` → 启用Kafka

## 架构设计

```
┌──────────────────────────────────────┐
│    ingot-social-common (公共基础)    │
│                                      │
│  ┌────────────────────────────────┐ │
│  │  SocialConfigProperties        │ │
│  │  - messageQueue: redis/kafka   │ │
│  │  - redis.topic                 │ │
│  │  - kafka.topic                 │ │
│  └────────────────────────────────┘ │
│                                      │
│  ┌────────────────────────────────┐ │
│  │  SocialConfigChangedEvent      │ │
│  │  - socialType (社交类型)       │ │
│  │  - changeType                  │ │
│  │  - appId                       │ │
│  └────────────────────────────────┘ │
│                                      │
│  ┌────────────────────────────────┐ │
│  │  SocialConfigMessagePublisher  │ │
│  │  - Redis实现                   │ │
│  │  - Kafka实现                   │ │
│  └────────────────────────────────┘ │
└──────────────────────────────────────┘
           ▲              ▲
           │              │
    ┌──────┘              └──────┐
    │                            │
┌───┴─────────────┐    ┌─────────┴────────┐
│ ingot-social-   │    │ ingot-social-    │
│ wechat          │    │ qq (未来)        │
│                 │    │                  │
│ - 只监听微信类型 │    │ - 只监听QQ类型   │
│ - WxMaService   │    │ - QqService      │
└─────────────────┘    └──────────────────┘
```

## 快速开始

### 1. 添加依赖

#### 使用Redis（默认）

```gradle
dependencies {
    implementation project(':ingot-framework:ingot-social-common')
    // Redis会自动从ingot-data-redis传递进来
}
```

#### 使用Kafka

```gradle
dependencies {
    implementation project(':ingot-framework:ingot-social-common')
    implementation 'org.springframework.kafka:spring-kafka'  // 添加Kafka依赖
}
```

### 2. 配置

#### 使用Redis（默认）

```yaml
ingot:
  social:
    # 默认使用redis，可不配置
    message-queue: redis
    redis:
      topic: ingot:social:config:changed
```

#### 使用Kafka

```yaml
ingot:
  social:
    message-queue: kafka  # 切换到Kafka
    kafka:
      topic: ingot-social-config-changed
      group-id: ingot-social-config-group

# 还需要配置Kafka连接
spring:
  kafka:
    bootstrap-servers: localhost:9092
```

### 3. 使用消息发布器

```java
@Service
@RequiredArgsConstructor
public class YourService {
    private final SocialConfigMessagePublisher configMessagePublisher;
    
    public void notifyConfigChange(String appId) {
        // 通知微信配置变更
        configMessagePublisher.publishUpdate(
            SocialTypeEnum.WECHAT_MINI_PROGRAM, 
            appId
        );
        
        // 通知QQ配置变更
        configMessagePublisher.publishUpdate(
            SocialTypeEnum.QQ, 
            appId
        );
    }
}
```

## 核心组件

### SocialConfigProperties

配置属性类：

```java
@ConfigurationProperties(prefix = "ingot.social")
public class SocialConfigProperties {
    private MessageQueueType messageQueue = MessageQueueType.REDIS;
    private RedisConfig redis = new RedisConfig();
    private KafkaConfig kafka = new KafkaConfig();
}
```

### SocialConfigChangedEvent

配置变更事件，包含社交类型：

```java
public class SocialConfigChangedEvent extends ApplicationEvent {
    private final SocialTypeEnum socialType;  // ✅ 社交类型
    private final ConfigChangeType changeType;
    private final String appId;
}
```

### SocialConfigMessagePublisher

消息发布器接口：

```java
public interface SocialConfigMessagePublisher {
    void publishRefreshAll(SocialTypeEnum socialType);
    void publishAdd(SocialTypeEnum socialType, String appId);
    void publishUpdate(SocialTypeEnum socialType, String appId);
    void publishDelete(SocialTypeEnum socialType, String appId);
}
```

**实现类**：
- `RedisSocialConfigMessagePublisher` - Redis实现
- `KafkaSocialConfigMessagePublisher` - Kafka实现

### 自动配置

`SocialCommonConfiguration` 使用条件注解自动装配：

```java
// Redis发布器 - 当message-queue=redis且有StringRedisTemplate时创建
@ConditionalOnProperty(name = "message-queue", havingValue = "redis", matchIfMissing = true)
@ConditionalOnClass(name = "org.springframework.data.redis.core.StringRedisTemplate")

// Kafka发布器 - 当message-queue=kafka且有KafkaTemplate时创建
@ConditionalOnProperty(name = "message-queue", havingValue = "kafka")
@ConditionalOnClass(name = "org.springframework.kafka.core.KafkaTemplate")
```

## 消息流转

### 配置变更流程

```
PMS服务操作数据库
        ↓
SysSocialDetailsServiceImpl
        ↓
发布消息到MQ (带社交类型)
        ↓
   Redis/Kafka
        ↓
所有服务接收消息
        ↓
SocialConfigRedisMessageListener
        ↓
转换为本地事件 (SocialConfigChangedEvent)
        ↓
    发布到Spring
        ↓
各社交模块监听器 (按类型过滤)
        ↓
更新对应的配置
```

### 消息格式

```json
{
  "socialType": "WECHAT_MINI_PROGRAM",
  "changeType": "ADD",
  "appId": "wx123456",
  "timestamp": 1234567890
}
```

## 扩展指南

### 创建新的社交平台模块

例如创建 `ingot-social-qq` 模块：

#### 1. 添加依赖

```gradle
dependencies {
    api project(':ingot-framework:ingot-social-common')
    api 'com.qq:qq-connect-sdk:x.x.x'
}
```

#### 2. 创建配置管理器

```java
public class QqConfigManager {
    private final Map<String, QqConfig> configMap = new ConcurrentHashMap<>();
    
    public void initConfigs() {
        // 从PMS加载QQ类型的配置
    }
    
    public void refreshAllConfigs() {
        // 刷新配置
    }
}
```

#### 3. 创建事件监听器

```java
@Slf4j
@RequiredArgsConstructor
public class QqConfigChangedListener {
    private final QqConfigManager qqConfigManager;

    @Async
    @EventListener
    public void onConfigChanged(SocialConfigChangedEvent event) {
        // ✅ 只处理QQ类型
        if (event.getSocialType() != SocialTypeEnum.QQ) {
            return;
        }
        
        // 处理QQ配置变更
        switch (event.getChangeType()) {
            case REFRESH_ALL:
                qqConfigManager.refreshAllConfigs();
                break;
            // ...
        }
    }
}
```

#### 4. 创建配置类

```java
@Configuration
@RequiredArgsConstructor
public class QqConfiguration {
    
    @Bean
    public QqConfigManager qqConfigManager(...) {
        return new QqConfigManager(...);
    }
    
    @Bean
    public QqConfigChangedListener qqConfigChangedListener(QqConfigManager manager) {
        return new QqConfigChangedListener(manager);
    }
}
```

## 配置说明

### message-queue

**类型**：枚举（redis, kafka）  
**默认值**：redis

- `redis` - 使用Redis Pub/Sub（默认）
- `kafka` - 使用Kafka

### redis.topic

**类型**：string  
**默认值**：`ingot:social:config:changed`

Redis Pub/Sub 频道名称，所有服务必须保持一致。

### kafka.topic

**类型**：string  
**默认值**：`ingot-social-config-changed`

Kafka 主题名称。

### kafka.group-id

**类型**：string  
**默认值**：`ingot-social-config-group`

Kafka 消费组ID。

## 条件装配说明

### Redis模式（默认）

当满足以下条件时，自动创建Redis相关Bean：
1. `message-queue=redis` 或未配置（默认值）
2. 类路径中存在 `StringRedisTemplate` 类

### Kafka模式

当满足以下条件时，自动创建Kafka相关Bean：
1. `message-queue=kafka`
2. 类路径中存在 `KafkaTemplate` 类

## 使用示例

### 发布配置变更通知

```java
@Service
@RequiredArgsConstructor
public class SocialConfigService {
    private final SocialConfigMessagePublisher publisher;
    
    public void addWechatConfig(SysSocialDetails config) {
        // 保存到数据库
        save(config);
        
        // 通知所有服务
        publisher.publishAdd(
            SocialTypeEnum.WECHAT_MINI_PROGRAM, 
            config.getAppId()
        );
    }
}
```

### 监听配置变更

```java
@Component
@RequiredArgsConstructor
public class WechatConfigListener {
    
    @EventListener
    @Async
    public void onConfigChanged(SocialConfigChangedEvent event) {
        // 只处理微信类型
        if (event.getSocialType() == SocialTypeEnum.WECHAT_MINI_PROGRAM) {
            // 处理微信配置变更
            log.info("微信配置变更: {}", event.getAppId());
        }
    }
}
```

## 优势

### 1. 模块解耦

- 公共功能在common模块
- 各平台独立模块
- 互不影响

### 2. 按需加载

```gradle
// 只需要微信
dependencies {
    implementation project(':ingot-framework:ingot-social-wechat')
}

// 需要微信和QQ
dependencies {
    implementation project(':ingot-framework:ingot-social-wechat')
    implementation project(':ingot-framework:ingot-social-qq')
}
```

### 3. 类型过滤

- 微信模块只处理微信配置
- QQ模块只处理QQ配置  
- 互不干扰

### 4. 消息队列灵活切换

```yaml
# 使用Redis
ingot:
  social:
    message-queue: redis

# 切换到Kafka（需要有spring-kafka依赖）
ingot:
  social:
    message-queue: kafka
```

## 注意事项

1. **Topic一致性**
   - 所有服务的 `redis.topic` 或 `kafka.topic` 必须一致
   - 建议在配置中心统一管理

2. **环境隔离**
   - 不同环境使用不同的topic前缀
   - 避免跨环境消息干扰

3. **依赖要求**
   - 使用Redis：需要 `spring-boot-starter-data-redis`
   - 使用Kafka：需要 `spring-kafka`

4. **类型支持**
   - 确保 `SocialTypeEnum` 包含所有需要的社交类型
   - 新增类型时需要更新枚举

## 版本历史

### v1.0.0 (2025-12-07)
- ✨ 初始版本
- ✨ 支持Redis和Kafka消息队列
- ✨ 按社交类型通知和监听
- ✨ 自动条件装配
- ✨ 所有服务都可发布消息

---

**作者**：jy  
**创建时间**：2025-12-07
