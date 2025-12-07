package com.ingot.framework.social.common.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.commons.model.enums.SocialTypeEnum;
import com.ingot.framework.social.common.event.SocialConfigRedisMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * <p>Description  : 社交配置变更消息发布器 - Kafka实现.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/7.</p>
 * <p>Time         : 16:00.</p>
 */
@Slf4j
public class KafkaSocialConfigMessagePublisher implements SocialConfigMessagePublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public KafkaSocialConfigMessagePublisher(KafkaTemplate<String, String> kafkaTemplate,
                                            ObjectMapper objectMapper,
                                            String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Override
    public void publishRefreshAll(SocialTypeEnum socialType) {
        publish(SocialConfigRedisMessage.refreshAll(socialType.getValue()));
    }

    @Override
    public void publishAdd(SocialTypeEnum socialType, String appId) {
        publish(SocialConfigRedisMessage.add(socialType.getValue(), appId));
    }

    @Override
    public void publishUpdate(SocialTypeEnum socialType, String appId) {
        publish(SocialConfigRedisMessage.update(socialType.getValue(), appId));
    }

    @Override
    public void publishDelete(SocialTypeEnum socialType, String appId) {
        publish(SocialConfigRedisMessage.delete(socialType.getValue(), appId));
    }

    @Override
    public void publish(SocialConfigRedisMessage message) {
        try {
            String messageJson = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(topic, messageJson);
            log.info("KafkaSocialConfigMessagePublisher - 已发布配置变更消息到 [{}]: socialType={}, changeType={}, appId={}",
                    topic, message.getSocialType(), message.getChangeType(), message.getAppId());
        } catch (Exception e) {
            log.error("KafkaSocialConfigMessagePublisher - 发布配置变更消息失败", e);
        }
    }
}

