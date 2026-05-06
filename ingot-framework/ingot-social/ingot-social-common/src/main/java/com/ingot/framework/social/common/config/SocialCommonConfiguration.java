package com.ingot.framework.social.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.social.common.event.SocialConfigMessageHandler;
import com.ingot.framework.social.common.event.SocialConfigRedisMessageListener;
import com.ingot.framework.social.common.properties.SocialConfigProperties;
import com.ingot.framework.social.common.publisher.RedisSocialConfigMessagePublisher;
import com.ingot.framework.social.common.publisher.SocialConfigMessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * <p>Description  : 社交公共配置.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/7.</p>
 * <p>Time         : 16:10.</p>
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SocialConfigProperties.class)
@RequiredArgsConstructor
public class SocialCommonConfiguration {
    private final SocialConfigProperties socialConfigProperties;

    // ==================== 消息处理器 ====================

    /**
     * 社交配置消息处理器
     * 用于处理消息的公共逻辑
     */
    @Bean
    public SocialConfigMessageHandler socialConfigMessageHandler(
            ApplicationEventPublisher eventPublisher,
            ObjectMapper objectMapper) {
        return new SocialConfigMessageHandler(eventPublisher, objectMapper);
    }

    // ==================== Redis 实现（默认） ====================

    /**
     * Redis消息发布器（默认实现）
     * 如果服务没有自定义实现，则使用此默认实现
     */
    @Bean
    @ConditionalOnMissingBean(SocialConfigMessagePublisher.class)
    public SocialConfigMessagePublisher redisSocialConfigMessagePublisher(
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper) {
        String topic = socialConfigProperties.getRedis().getTopic();
        log.info("SocialCommonConfiguration - 初始化Redis消息发布器（默认），主题: {}", topic);
        return new RedisSocialConfigMessagePublisher(stringRedisTemplate, objectMapper, topic);
    }

    /**
     * Redis消息监听器
     */
    @Bean
    public SocialConfigRedisMessageListener socialConfigRedisMessageListener(
            SocialConfigMessageHandler messageHandler) {
        log.info("SocialCommonConfiguration - 注册Redis消息监听器");
        return new SocialConfigRedisMessageListener(messageHandler);
    }

    /**
     * 把 social 配置变更监听器注册到框架统一的 {@link RedisMessageListenerContainer}
     * （由 {@code InRedisMessageConfiguration#redisContainer} 提供）。
     * <p>
     * 不再自建独立容器，避免与全局唯一容器并存：
     * <ul>
     *     <li>消除 {@code RedisMessageListenerContainer} 多 bean 注入冲突
     *         （如 {@code ingot-event-bus} 注入容器时报错）；</li>
     *     <li>节省一条 Redis 订阅连接（每个 container 独占一条 PSUBSCRIBE）。</li>
     * </ul>
     * 后续 social 迁移到 {@code ingot-event-bus} 后，此处可整体下线。
     * </p>
     */
    @Bean
    public InitializingBean socialConfigListenerRegistrar(
            RedisMessageListenerContainer container,
            SocialConfigRedisMessageListener messageListener) {
        return () -> {
            String topic = socialConfigProperties.getRedis().getTopic();
            container.addMessageListener(messageListener, new ChannelTopic(topic));
            log.info("SocialCommonConfiguration - 已向通用 RedisMessageListenerContainer 注册 social 监听，topic: {}", topic);
        };
    }
}
