# Ingot Social Wechat - 微信社交集成模块

## 概述

`ingot-social-wechat` 是微信小程序等微信能力的集成模块，依赖 `ingot-social-common`。配置变更通过 **`SocialConfigChangedEvent`** 驱动本地 `WxMaConfigManager` 热更新。

## 核心特性

### 1. 基于 ingot-social-common

- 公共跨节点通知由 **`InvalidationBus` + `SocialInvalidationEvent`** 完成（见 common 模块 README）。
- 本模块**只处理** `SocialTypeEnum.WECHAT_MINI_PROGRAM`，忽略其它 `socialType`。

### 2. 动态配置更新

- 支持热更新，无需重启（实际延迟取决于总线与应用负载）。
- 与字典等域共用 **`ingot.event-bus`**；请保证各实例 **`topic-prefix` 一致**。

### 3. 多应用 / 多租户

- 支持多个 `appId`（小程序）并存；通过 `WxMaService.switchoverTo(appId)` 切换。

## 依赖

```gradle
dependencies {
    implementation project(':ingot-framework:ingot-social-wechat')
}
```

会传递依赖 `ingot-social-common`（及 `ingot-event-bus` API）。运行环境仍需 **Redis + 统一 `RedisMessageListenerContainer`** 以装配 `InvalidationBus`（与平台其它服务一致）。

## 配置示例

```yaml
ingot:
  event-bus:
    redis:
      topic-prefix: in:bus   # 与 PMS、AUTH 等保持一致

# 已废弃，勿再依赖：
# ingot.social.redis.topic
```

## 使用方式

### 1. 注入 WxMaService

```java
@Service
@RequiredArgsConstructor
public class WechatLoginService {
    private final WxMaService wxMaService;
    
    public String login(String appId, String code) {
        WxMaService service = wxMaService.switchoverTo(appId);
        WxMaJscode2SessionResult session = service.getUserService().getSessionInfo(code);
        return session.getOpenid();
    }
}
```

### 2. 手动刷新 HTTP API

```bash
# 广播刷新（依赖 SocialConfigMessagePublisher，默认走 InvalidationBus）
POST /social/wechat/config/refresh/all

# 仅当前实例
POST /social/wechat/config/refresh/local

GET /social/wechat/config/status
```

## 事件监听（本模块内部逻辑说明）

监听器仅处理微信小程序类型：

```java
@EventListener
public void onConfigChanged(SocialConfigChangedEvent event) {
    if (event.getSocialType() != SocialTypeEnum.WECHAT_MINI_PROGRAM) {
        return;
    }
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

## 在 PMS 中维护配置

`SysSocialDetailsServiceImpl` 在保存 / 更新 / 删除成功后会调用 **`SocialConfigMessagePublisher`**，默认实现会向 **`InvalidationBus`** 发布 `SocialInvalidationEvent`，各.consumer 再收到 `SocialConfigChangedEvent` 刷新内存配置。

## 注意事项

1. **`topic-prefix` 集群一致**，否则收不到对端社交失效广播。
2. **类型过滤**：本模块监听器已按 `WECHAT_MINI_PROGRAM` 过滤。
3. **依赖**：一般无需再手写 `ingot-social-common` 依赖；引入 `ingot-social-wechat` 即可。

## 相关文档

- [ingot-social-common/README.md](../ingot-social-common/README.md)
- [ingot-social-common/EXTENSION_GUIDE.md](../ingot-social-common/EXTENSION_GUIDE.md)
- [docs/dict/ARCHITECTURE.md](../../../docs/dict/ARCHITECTURE.md)（失效总线通用说明）

---

**版本**：v2.1（InvalidationBus 对齐）  
**创建时间**：2025-12-07  
**作者**：jy
