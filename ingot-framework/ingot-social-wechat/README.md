# Ingot Social Wechat - 微信社交集成模块

## 概述

`ingot-social-wechat` 是一个微信社交功能的集成模块，主要用于封装微信小程序相关的API。该模块支持**多租户、多应用**的微信小程序配置，并提供**动态配置更新**能力，无需重启服务即可感知配置变更。

## 核心特性

### 1. 统一配置管理
- 使用 PMS 服务的 `SysSocialDetails` 作为唯一配置数据源
- 所有服务统一从 PMS 获取社交配置

### 2. 动态配置更新
- 支持配置的热更新，无需重启服务
- 当 PMS 中新增/修改/删除社交配置时，自动通知所有服务实例
- 默认基于 Redis 发布订阅机制，支持扩展到 Kafka 等消息队列

### 3. 多租户支持
- 支持同时配置多个微信小程序应用
- 每个租户可以有独立的小程序配置
- 通过 `appId` 自动切换不同的微信小程序服务

### 4. 灵活可扩展
- 统一在 `WechatConfiguration` 中注册所有Bean
- 支持多种消息队列（Redis、Kafka等）
- 通过配置区分发布者（PMS）和消费者（其他服务）

## 架构设计

### 整体架构图

```
┌─────────────────────────────────────────────────────────────┐
│                         PMS Service                          │
│  ┌───────────────────────────────────────────────────────┐  │
│  │          SysSocialDetailsServiceImpl                   │  │
│  │  - save/update/delete 操作后发布Redis消息              │  │
│  └───────────────────┬───────────────────────────────────┘  │
└────────────────────────┼─────────────────────────────────────┘
                         │ publish
                         ▼
              ┌──────────────────────┐
              │   Redis Pub/Sub      │
              │  Channel: ingot:     │
              │  social:config:      │
              │  changed             │
              └──────────┬───────────┘
                         │ subscribe
        ┌────────────────┼────────────────┐
        │                │                │
        ▼                ▼                ▼
┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│   Member    │  │     PMS     │  │   Gateway   │
│  Service    │  │   Service   │  │   Service   │
│             │  │             │  │             │
│ ┌─────────┐ │  │ ┌─────────┐ │  │ ┌─────────┐ │
│ │WxMaConfig││  │ │WxMaConfig││  │ │WxMaConfig││
│ │ Manager  ││  │ │ Manager  ││  │ │ Manager  ││
│ │          ││  │ │          ││  │ │          ││
│ │- init    ││  │ │- init    ││  │ │- init    ││
│ │- refresh ││  │ │- refresh ││  │ │- refresh ││
│ │- add/    ││  │ │- add/    ││  │ │- add/    ││
│ │  update/ ││  │ │  update/ ││  │ │  update/ ││
│ │  remove  ││  │ │  remove  ││  │ │  remove  ││
│ └─────────┘ │  │ └─────────┘ │  │ └─────────┘ │
└─────────────┘  └─────────────┘  └─────────────┘
```

### 核心组件

#### 1. WxMaConfigManager（配置管理器）
负责微信小程序配置的生命周期管理：
- `initConfigs()` - 初始化加载配置
- `refreshAllConfigs()` - 刷新所有配置
- `addOrUpdateConfig()` - 添加或更新单个配置
- `removeConfig()` - 移除配置
- `hasConfig()` - 检查配置是否存在

#### 2. 事件驱动机制

##### 本地事件
- `SocialConfigChangedEvent` - 本地配置变更事件
- `SocialConfigChangedListener` - 本地事件监听器

##### Redis消息
- `SocialConfigRedisMessage` - Redis消息体
- `SocialConfigRedisMessageListener` - Redis消息监听器
- `SocialConfigChangePublisher` - 消息发布器

#### 3. 配置通知流程

```
PMS数据库操作 → SysSocialDetailsServiceImpl
                     ↓
            发布Redis消息 (publish)
                     ↓
              Redis Pub/Sub
                     ↓
     所有服务实例接收消息 (subscribe)
                     ↓
       SocialConfigRedisMessageListener
                     ↓
       转换为本地事件并发布
                     ↓
       SocialConfigChangedListener
                     ↓
          WxMaConfigManager
                     ↓
          更新WxMaService配置
```

## 使用指南

### 1. 依赖配置

在需要使用微信小程序功能的服务中添加依赖：

```gradle
dependencies {
    implementation project(':ingot-framework:ingot-social-wechat')
}
```

### 2. 服务配置

#### PMS服务（配置发布者）

```yaml
# application.yml
ingot:
  social:
    message-queue: redis
    redis:
      topic: in:social:config:changed
```

#### Member服务（配置消费者）

```yaml
# application.yml
ingot:
  social:
    message-queue: redis
    redis:
      topic: in:social:config:changed
```

### 3. 配置管理

#### 添加社交配置
在 PMS 服务中通过 `SysSocialDetailsService` 管理配置：

```java
SysSocialDetails socialDetails = new SysSocialDetails();
socialDetails.setAppId("wx1234567890");
socialDetails.setAppSecret("your_app_secret");
socialDetails.setType(SocialTypeEnum.WECHAT_MINI_PROGRAM);
socialDetails.setName("测试小程序");
socialDetails.setTenantId(1L);

// 保存后会自动通知所有服务实例
sysSocialDetailsService.save(socialDetails);
```

#### 更新配置
```java
SysSocialDetails socialDetails = sysSocialDetailsService.getById(id);
socialDetails.setAppSecret("new_app_secret");

// 更新后会自动通知所有服务实例
sysSocialDetailsService.updateById(socialDetails);
```

#### 删除配置
```java
// 删除后会自动通知所有服务实例
sysSocialDetailsService.removeById(id);
```

### 3. 使用WxMaService

在业务代码中注入并使用 `WxMaService`：

```java
@Service
@RequiredArgsConstructor
public class WechatBusinessService {
    private final WxMaService wxMaService;
    
    public void doSomething(String appId) {
        // 切换到指定的小程序配置
        WxMaService service = wxMaService.switchoverTo(appId);
        
        // 使用微信API
        WxMaJscode2SessionResult session = service.getUserService()
            .getSessionInfo(code);
    }
}
```

### 4. 手动刷新配置

#### 刷新本地配置（仅当前服务实例）
```bash
POST /social/wechat/config/refresh/local
```

#### 刷新所有服务实例配置（通过Redis广播）
```bash
POST /social/wechat/config/refresh/all
```

#### 查看配置状态
```bash
GET /social/wechat/config/status
```

返回示例：
```json
{
  "code": "0000",
  "data": {
    "count": 3,
    "appIds": [
      "wx1234567890",
      "wx0987654321",
      "wx1111111111"
    ],
    "timestamp": 1638888888888
  }
}
```

## 架构设计

### 角色区分

- **PMS服务（发布者）**：
  - 当数据库配置变更时，发布消息到消息队列
  - 其他服务接收消息后自动更新配置

- **Member/Gateway服务（消费者）**：
  - 只监听消息队列，不发布消息
  - 接收到消息后自动更新本地配置

### 消息队列支持

- **默认**：Redis Pub/Sub
- **扩展**：支持 Kafka 等（通过实现 `SocialConfigMessagePublisher` 接口）

### 配置变更类型

```java
public enum ConfigChangeType {
    ADD,         // 添加配置
    UPDATE,      // 更新配置
    DELETE,      // 删除配置
    REFRESH_ALL  // 刷新所有配置
}
```

### Redis频道
配置变更消息发布到以下Redis频道（可配置）：
```
in:social:config:changed
```

### 线程安全
- `WxMaConfigManager` 使用 `ConcurrentHashMap` 存储配置，保证线程安全
- 配置的添加、更新、删除操作使用 `synchronized` 关键字保证原子性

### 异步处理
配置变更事件监听器使用 `@Async` 注解，异步处理配置更新，不阻塞主流程。

### Bean注册
所有Bean都在 `WechatConfiguration` 中统一注册，通过条件注解控制创建：
- `@ConditionalOnProperty` - 根据配置决定是否创建
- `@ConditionalOnBean` - 根据依赖Bean是否存在决定是否创建

## 注意事项

1. **Redis依赖**：该模块依赖Redis，请确保Redis服务正常运行
2. **配置延迟**：通过Redis通知的配置更新有轻微延迟（通常<100ms）
3. **配置一致性**：如果Redis不可用，新增服务实例会在启动时从PMS加载配置
4. **权限控制**：手动刷新配置接口应添加适当的权限控制
5. **监控建议**：建议监控配置更新失败的情况，必要时手动触发刷新
6. **Topic一致性**：所有服务的 `redis.topic` 配置必须保持一致


