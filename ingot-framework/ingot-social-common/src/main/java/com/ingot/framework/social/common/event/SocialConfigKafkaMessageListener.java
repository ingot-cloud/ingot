package com.ingot.framework.social.common.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;

/**
 * <p>Description  : 社交配置Kafka消息监听器.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/7.</p>
 * <p>Time         : 17:10.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class SocialConfigKafkaMessageListener {
    private final SocialConfigMessageHandler messageHandler;

    @KafkaListener(topics = "${ingot.social.kafka.topic:ingot-social-config-changed}", 
                   groupId = "${ingot.social.kafka.group-id:ingot-social-config-group}")
    public void onMessage(String message) {
        messageHandler.handleMessage(message, this);
    }
}
