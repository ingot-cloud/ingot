package com.ingot.framework.social.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.social.common.event.SocialConfigKafkaMessageListener;
import com.ingot.framework.social.common.event.SocialConfigMessageHandler;
import com.ingot.framework.social.common.event.SocialConfigRedisMessageListener;
import com.ingot.framework.social.common.properties.SocialConfigProperties;
import com.ingot.framework.social.common.publisher.KafkaSocialConfigMessagePublisher;
import com.ingot.framework.social.common.publisher.RedisSocialConfigMessagePublisher;
import com.ingot.framework.social.common.publisher.SocialConfigMessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.kafka.core.KafkaTemplate;

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
     * 用于处理Redis和Kafka消息的公共逻辑
     */
    @Bean
    public SocialConfigMessageHandler socialConfigMessageHandler(
            ApplicationEventPublisher eventPublisher,
            ObjectMapper objectMapper) {
        return new SocialConfigMessageHandler(eventPublisher, objectMapper);
    }

    // ==================== Redis 消息发布器 ====================

    /**
     * Redis消息发布器
     * 条件：1. 使用Redis作为消息队列 2. 类路径中存在Redis
     */
    @Bean
    @ConditionalOnProperty(prefix = "ingot.social", name = "message-queue", havingValue = "redis", matchIfMissing = true)
    @ConditionalOnClass(StringRedisTemplate.class)
    public SocialConfigMessagePublisher redisSocialConfigMessagePublisher(
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper) {
        String topic = socialConfigProperties.getRedis().getTopic();
        log.info("SocialCommonConfiguration - 初始化Redis消息发布器，主题: {}", topic);
        return new RedisSocialConfigMessagePublisher(stringRedisTemplate, objectMapper, topic);
    }

    /**
     * Redis消息监听器
     * 条件：使用Redis作为消息队列且类路径中存在Redis
     */
    @Bean
    @ConditionalOnProperty(prefix = "ingot.social", name = "message-queue", havingValue = "redis", matchIfMissing = true)
    @ConditionalOnClass(RedisConnectionFactory.class)
    public SocialConfigRedisMessageListener socialConfigRedisMessageListener(
            SocialConfigMessageHandler messageHandler) {
        return new SocialConfigRedisMessageListener(messageHandler);
    }

    /**
     * Redis消息监听容器
     * 条件：使用Redis作为消息队列且Redis可用
     */
    @Bean
    @ConditionalOnProperty(prefix = "ingot.social", name = "message-queue", havingValue = "redis", matchIfMissing = true)
    @ConditionalOnClass(RedisConnectionFactory.class)
    public RedisMessageListenerContainer socialConfigRedisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            SocialConfigRedisMessageListener messageListener) {
        
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        
        String topic = socialConfigProperties.getRedis().getTopic();
        container.addMessageListener(messageListener, new ChannelTopic(topic));
        
        log.info("SocialCommonConfiguration - Redis消息监听器已配置，主题: {}", topic);
        return container;
    }

    // ==================== Kafka 消息发布器 ====================

    /**
     * Kafka消息发布器
     * 条件：1. 使用Kafka作为消息队列 2. 类路径中存在Kafka
     */
    @Bean
    @ConditionalOnProperty(prefix = "ingot.social", name = "message-queue", havingValue = "kafka")
    @ConditionalOnClass(KafkaTemplate.class)
    public SocialConfigMessagePublisher kafkaSocialConfigMessagePublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper) {
        String topic = socialConfigProperties.getKafka().getTopic();
        log.info("SocialCommonConfiguration - 初始化Kafka消息发布器，主题: {}", topic);
        return new KafkaSocialConfigMessagePublisher(kafkaTemplate, objectMapper, topic);
    }

    /**
     * Kafka消息监听器
     * 条件：使用Kafka作为消息队列且类路径中存在Kafka
     */
    @Bean
    @ConditionalOnProperty(prefix = "ingot.social", name = "message-queue", havingValue = "kafka")
    @ConditionalOnClass(KafkaTemplate.class)
    public SocialConfigKafkaMessageListener socialConfigKafkaMessageListener(
            SocialConfigMessageHandler messageHandler) {
        log.info("SocialCommonConfiguration - Kafka消息监听器已配置");
        return new SocialConfigKafkaMessageListener(messageHandler);
    }
}
