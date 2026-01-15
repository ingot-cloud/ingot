# Ingot Social Wechat - 微信社交集成模块

## 概述

`ingot-social-wechat` 是微信社交功能的集成模块，依赖于 `ingot-social-common` 公共模块。

## 核心特性

### 1. 基于 ingot-social-common
- 依赖公共社交模块
- 只监听微信类型的配置变更
- 自动过滤其他社交平台的消息

### 2. 动态配置更新
- 支持配置的热更新，无需重启服务
- 配置变更后自动通知（延迟<100ms）
- 支持Redis/Kafka多种消息队列

### 3. 多租户支持
- 支持同时配置多个微信小程序应用
- 每个租户可以有独立的小程序配置
- 通过 `appId` 自动切换配置

## 依赖配置

```gradle
dependencies {
    // 会自动依赖 ingot-social-common
    implementation project(':ingot-framework:ingot-social-wechat')
}
```

## 配置示例

```yaml
ingot:
  social:
    message-queue: redis  # 或 kafka
    redis:
      topic: in:social:config:changed
```

## 使用方式

### 1. 注入 WxMaService

```java
@Service
@RequiredArgsConstructor
public class WechatLoginService {
    private final WxMaService wxMaService;
    
    public String login(String appId, String code) {
        // 切换到指定小程序
        WxMaService service = wxMaService.switchoverTo(appId);
        
        // 调用微信API
        WxMaJscode2SessionResult session = service.getUserService()
            .getSessionInfo(code);
        
        return session.getOpenid();
    }
}
```

### 2. 手动刷新配置

```bash
# 刷新所有服务实例（广播）
POST /social/wechat/config/refresh/all

# 刷新当前服务实例
POST /social/wechat/config/refresh/local

# 查看配置状态
GET /social/wechat/config/status
```

## 事件监听

微信模块的监听器只处理微信类型的配置变更：

```java
@EventListener
public void onConfigChanged(SocialConfigChangedEvent event) {
    // 只处理微信小程序类型
    if (event.getSocialType() != SocialTypeEnum.WECHAT_MINI_PROGRAM) {
        return;
    }
    
    // 处理微信配置变更
    switch (event.getChangeType()) {
        case ADD:
        case UPDATE:
            wxMaConfigManager.refreshAllConfigs();
            break;
        case DELETE:
            wxMaConfigManager.removeConfig(event.getAppId());
            break;
        case REFRESH_ALL:
            wxMaConfigManager.refreshAllConfigs();
            break;
    }
}
```

## 与其他社交模块协同

### 同时使用微信和QQ

```gradle
dependencies {
    implementation project(':ingot-framework:ingot-social-wechat')
    implementation project(':ingot-framework:ingot-social-qq')
}
```

**效果**：
- 微信配置变更 → 只有微信模块响应
- QQ配置变更 → 只有QQ模块响应
- 互不干扰

### 在PMS中管理配置

```java
@Service
@RequiredArgsConstructor
public class SocialConfigManagementService {
    private final SysSocialDetailsService socialDetailsService;
    
    // 添加微信配置 - 自动通知微信模块
    public void addWechatConfig(SysSocialDetails config) {
        config.setType(SocialTypeEnum.WECHAT_MINI_PROGRAM);
        socialDetailsService.save(config);
        // 保存后自动发布消息，微信模块自动更新
    }
    
    // 添加QQ配置 - 自动通知QQ模块
    public void addQqConfig(SysSocialDetails config) {
        config.setType(SocialTypeEnum.QQ);
        socialDetailsService.save(config);
        // 保存后自动发布消息，QQ模块自动更新
    }
}
```

## 核心组件

### WxMaConfigManager

微信小程序配置管理器：
- `initConfigs()` - 初始化配置
- `refreshAllConfigs()` - 刷新所有配置
- `addOrUpdateConfig()` - 添加或更新配置
- `removeConfig()` - 移除配置

### WechatConfigChangedListener

微信配置变更监听器：
- 只监听 `SocialTypeEnum.WECHAT_MINI_PROGRAM` 类型
- 自动过滤其他社交平台的消息
- 异步处理，不阻塞主流程

## 注意事项

1. **类型过滤**
   - 监听器会自动过滤非微信类型的事件
   - 只处理 `WECHAT_MINI_PROGRAM` 类型

2. **配置同步**
   - 所有服务的 `redis.topic` 必须一致
   - 建议在配置中心统一管理

3. **依赖关系**
   - 自动依赖 `ingot-social-common`
   - 无需手动添加common依赖

## 相关文档

- [ingot-social-common/README.md](../ingot-social-common/README.md) - 公共模块说明

---

**版本**：v2.0.0  
**创建时间**：2025-12-07  
**作者**：jy
