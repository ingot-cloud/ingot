package com.ingot.framework.social.wechat.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

/**
 * <p>Description  : 社交配置Redis消息监听器.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/6.</p>
 * <p>Time         : 17:25.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class SocialConfigRedisMessageListener implements MessageListener {
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody());
            log.debug("SocialConfigRedisMessageListener - 接收到Redis消息: {}", body);

            SocialConfigRedisMessage redisMessage = objectMapper.readValue(body, SocialConfigRedisMessage.class);

            // 转换为本地事件并发布
            SocialConfigChangedEvent.ConfigChangeType changeType =
                    SocialConfigChangedEvent.ConfigChangeType.valueOf(redisMessage.getChangeType());

            SocialConfigChangedEvent event;
            if (changeType == SocialConfigChangedEvent.ConfigChangeType.REFRESH_ALL) {
                event = new SocialConfigChangedEvent(this);
            } else {
                event = new SocialConfigChangedEvent(this, changeType, redisMessage.getAppId());
            }

            eventPublisher.publishEvent(event);
            log.info("SocialConfigRedisMessageListener - 已发布本地事件: type={}, appId={}",
                    changeType, redisMessage.getAppId());

        } catch (Exception e) {
            log.error("SocialConfigRedisMessageListener - 处理Redis消息失败", e);
        }
    }
}


