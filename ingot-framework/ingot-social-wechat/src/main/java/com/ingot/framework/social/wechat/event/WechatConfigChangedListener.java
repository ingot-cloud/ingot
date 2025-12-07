package com.ingot.framework.social.wechat.event;

import com.ingot.framework.commons.model.enums.SocialTypeEnum;
import com.ingot.framework.social.common.event.SocialConfigChangedEvent;
import com.ingot.framework.social.wechat.core.WxMaConfigManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

/**
 * <p>Description  : 微信配置变更事件监听器.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/7.</p>
 * <p>Time         : 16:20.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class WechatConfigChangedListener {
    private final WxMaConfigManager wxMaConfigManager;

    /**
     * 监听配置变更事件
     * 只处理微信小程序类型的配置
     *
     * @param event 配置变更事件
     */
    @Async
    @EventListener
    public void onConfigChanged(SocialConfigChangedEvent event) {
        // 只处理微信小程序类型
        if (event.getSocialType() != SocialTypeEnum.WECHAT_MINI_PROGRAM) {
            return;
        }
        
        log.info("WechatConfigChangedListener - 接收到微信配置变更事件: changeType={}, appId={}",
                event.getChangeType(), event.getAppId());

        try {
            switch (event.getChangeType()) {
                case REFRESH_ALL:
                    wxMaConfigManager.refreshAllConfigs();
                    break;
                case DELETE:
                    if (event.getAppId() != null) {
                        wxMaConfigManager.removeConfig(event.getAppId());
                    }
                    break;
                case ADD:
                case UPDATE:
                    // 对于ADD和UPDATE，都通过刷新全部配置来处理
                    // 因为需要从远程获取最新的配置信息
                    wxMaConfigManager.refreshAllConfigs();
                    break;
                default:
                    log.warn("WechatConfigChangedListener - 未知的变更类型: {}", event.getChangeType());
            }
        } catch (Exception e) {
            log.error("WechatConfigChangedListener - 处理配置变更事件失败", e);
        }
    }
}

