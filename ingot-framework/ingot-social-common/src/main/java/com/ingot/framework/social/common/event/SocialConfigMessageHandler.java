package com.ingot.framework.social.common.event;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    /**
     * 处理消息字符串，转换为事件并发布
     *
     * @param messageBody 消息内容
     * @param source 事件源（用于标识消息来源：Redis/Kafka）
     */
    public void handleMessage(String messageBody, Object source) {
        try {
            log.debug("SocialConfigMessageHandler - 接收到消息: {}", messageBody);

            // 解析消息
            SocialConfigRedisMessage message = objectMapper.readValue(messageBody, SocialConfigRedisMessage.class);

            // 转换为本地事件
            SocialConfigChangedEvent event = convertToEvent(message, source);

            // 发布事件
            eventPublisher.publishEvent(event);

            log.info("SocialConfigMessageHandler - 已发布本地事件: socialType={}, changeType={}, appId={}",
                    event.getSocialType(), event.getChangeType(), event.getAppId());

        } catch (Exception e) {
            log.error("SocialConfigMessageHandler - 处理消息失败: {}", messageBody, e);
        }
    }

    /**
     * 将消息转换为事件
     *
     * @param message 消息对象
     * @param source 事件源
     * @return 配置变更事件
     */
    private SocialConfigChangedEvent convertToEvent(SocialConfigRedisMessage message, Object source) {
        // 解析社交类型
        SocialTypeEnum socialType = SocialTypeEnum.get(message.getSocialType());
        
        // 解析变更类型
        SocialConfigChangedEvent.ConfigChangeType changeType =
                SocialConfigChangedEvent.ConfigChangeType.valueOf(message.getChangeType());

        // 根据变更类型创建不同的事件
        if (changeType == SocialConfigChangedEvent.ConfigChangeType.REFRESH_ALL) {
            return new SocialConfigChangedEvent(source, socialType);
        } else {
            return new SocialConfigChangedEvent(source, socialType, changeType, message.getAppId());
        }
    }
}

