package com.ingot.framework.social.wechat.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.social.wechat.event.SocialConfigRedisMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * <p>Description  : 社交配置变更消息发布器 - Redis实现.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/6.</p>
 * <p>Time         : 17:35.</p>
 */
@Slf4j
public class RedisSocialConfigMessagePublisher implements SocialConfigMessagePublisher {
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public RedisSocialConfigMessagePublisher(StringRedisTemplate stringRedisTemplate, 
                                            ObjectMapper objectMapper,
                                            String topic) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Override
    public void publishRefreshAll() {
        publish(SocialConfigRedisMessage.refreshAll());
    }

    @Override
    public void publishAdd(String appId) {
        publish(SocialConfigRedisMessage.add(appId));
    }

    @Override
    public void publishUpdate(String appId) {
        publish(SocialConfigRedisMessage.update(appId));
    }

    @Override
    public void publishDelete(String appId) {
        publish(SocialConfigRedisMessage.delete(appId));
    }

    @Override
    public void publish(SocialConfigRedisMessage message) {
        try {
            String messageJson = objectMapper.writeValueAsString(message);
            stringRedisTemplate.convertAndSend(topic, messageJson);
            log.info("RedisSocialConfigMessagePublisher - 已发布配置变更消息到 [{}]: type={}, appId={}",
                    topic, message.getChangeType(), message.getAppId());
        } catch (Exception e) {
            log.error("RedisSocialConfigMessagePublisher - 发布配置变更消息失败", e);
        }
    }
}


