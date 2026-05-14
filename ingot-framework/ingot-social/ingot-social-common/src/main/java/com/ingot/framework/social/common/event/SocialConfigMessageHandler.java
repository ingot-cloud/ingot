package com.ingot.framework.social.common.event;

import com.ingot.framework.commons.model.enums.SocialTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;

/**
 * <p>Description  : 社交配置消息处理器，处理消息解析和事件发布.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/7.</p>
 * <p>Time         : 17:00.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class SocialConfigMessageHandler {
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 处理经 {@link com.ingot.framework.eventbus.InvalidationBus} 反序列化后的失效事件，发布本地 {@link SocialConfigChangedEvent}。
     */
    public void handleInvalidation(SocialInvalidationEvent invalidation, Object source) {
        if (invalidation == null) {
            return;
        }
        try {
            SocialConfigChangedEvent event = convertToEvent(invalidation, source);
            eventPublisher.publishEvent(event);
            log.info("SocialConfigMessageHandler - 已发布本地事件: socialType={}, changeType={}, appId={}",
                    event.getSocialType(), event.getChangeType(), event.getAppId());
        } catch (Exception e) {
            log.error("SocialConfigMessageHandler - 处理失效事件失败: {}", invalidation, e);
        }
    }

    private SocialConfigChangedEvent convertToEvent(SocialInvalidationEvent message, Object source) {
        SocialTypeEnum socialType = SocialTypeEnum.get(message.getSocialType());
        SocialConfigChangedEvent.ConfigChangeType changeType =
                SocialConfigChangedEvent.ConfigChangeType.valueOf(message.getChangeType());
        if (changeType == SocialConfigChangedEvent.ConfigChangeType.REFRESH_ALL) {
            return new SocialConfigChangedEvent(source, socialType);
        }
        return new SocialConfigChangedEvent(source, socialType, changeType, message.getAppId());
    }
}

