package com.ingot.framework.eventbus.redis;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.eventbus.InvalidationBus;
import com.ingot.framework.eventbus.InvalidationEvent;
import com.ingot.framework.eventbus.Subscription;
import com.ingot.framework.eventbus.config.EventBusProperties;
import com.ingot.framework.eventbus.support.EventTypeResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * 基于 Redis Pub/Sub 的 {@link InvalidationBus} 默认实现。
 *
 * @author jy
 * @since 2026/4/27
 */
@Slf4j
public class RedisInvalidationBus implements InvalidationBus {

    private final StringRedisTemplate redisTemplate;
    private final RedisMessageListenerContainer listenerContainer;
    private final ObjectMapper objectMapper;
    private final EventBusProperties properties;
    private final String origin;

    public RedisInvalidationBus(StringRedisTemplate redisTemplate,
                                RedisMessageListenerContainer listenerContainer,
                                ObjectMapper objectMapper,
                                EventBusProperties properties,
                                String origin) {
        this.redisTemplate = redisTemplate;
        this.listenerContainer = listenerContainer;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.origin = origin;
    }

    @Override
    public <E extends InvalidationEvent> void publish(E event) {
        if (event == null) {
            return;
        }
        if (event.getOrigin() == null || event.getOrigin().isBlank()) {
            event.setOrigin(origin);
        }
        if (event.getTimestamp() == 0L) {
            event.setTimestamp(System.currentTimeMillis());
        }
        String channel = EventTypeResolver.channel(properties.getRedis().getTopicPrefix(), event.getClass());
        try {
            String body = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(channel, body);
            if (log.isDebugEnabled()) {
                log.debug("[EventBus] publish channel={} body={}", channel, body);
            }
        } catch (Exception e) {
            log.warn("[EventBus] publish failed channel={}", channel, e);
        }
    }

    @Override
    public <E extends InvalidationEvent> Subscription subscribe(Class<E> eventType, Consumer<E> handler) {
        String channel = EventTypeResolver.channel(properties.getRedis().getTopicPrefix(), eventType);
        ChannelTopic topic = new ChannelTopic(channel);
        MessageListener listener = (message, pattern) -> {
            try {
                byte[] body = message.getBody();
                if (body == null || body.length == 0) {
                    return;
                }
                E event = objectMapper.readValue(body, eventType);
                if (event == null) {
                    return;
                }
                if (Objects.equals(origin, event.getOrigin())) {
                    if (log.isTraceEnabled()) {
                        log.trace("[EventBus] skip self channel={}", channel);
                    }
                    return;
                }
                handler.accept(event);
            } catch (Exception e) {
                log.warn("[EventBus] handle failed channel={} body={}",
                        channel, new String(message.getBody(), StandardCharsets.UTF_8), e);
            }
        };
        listenerContainer.addMessageListener(listener, topic);
        log.info("[EventBus] subscribed channel={}", channel);
        return () -> {
            listenerContainer.removeMessageListener(listener, topic);
            log.info("[EventBus] unsubscribed channel={}", channel);
        };
    }
}
