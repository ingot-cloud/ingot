package com.ingot.framework.social.common.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

/**
 * <p>Description  : 社交配置Redis消息监听器.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/7.</p>
 * <p>Time         : 17:05.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class SocialConfigRedisMessageListener implements MessageListener {
    private final SocialConfigMessageHandler messageHandler;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String body = new String(message.getBody());
        messageHandler.handleMessage(body, this);
    }
}
