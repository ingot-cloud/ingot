package com.ingot.framework.social.wechat.config;

import cn.binarywang.wx.miniapp.api.WxMaQrcodeService;
import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.cloud.pms.api.rpc.RemotePmsSocialDetailsService;
import com.ingot.framework.social.wechat.api.WxMaConfigRefreshAPI;
import com.ingot.framework.social.wechat.core.WxMaConfigManager;
import com.ingot.framework.social.wechat.core.WxMaServiceHelper;
import com.ingot.framework.social.wechat.event.SocialConfigChangedListener;
import com.ingot.framework.social.wechat.event.SocialConfigRedisMessageListener;
import com.ingot.framework.social.wechat.properties.SocialConfigProperties;
import com.ingot.framework.social.wechat.properties.SocialWechatProperties;
import com.ingot.framework.social.wechat.publisher.RedisSocialConfigMessagePublisher;
import com.ingot.framework.social.wechat.publisher.SocialConfigMessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * <p>Description  : WechatConfiguration - 支持动态配置更新.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/6.</p>
 * <p>Time         : 15:44.</p>
 */
@Slf4j
@EnableAsync
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({SocialWechatProperties.class, SocialConfigProperties.class})
@Import(WxMaConfigInitializer.class)
@RequiredArgsConstructor
public class WechatConfiguration implements InitializingBean {

    private final RemotePmsSocialDetailsService remotePmsSocialDetailsService;
    private final SocialConfigProperties socialConfigProperties;

    // ==================== 核心服务 ====================

    /**
     * 微信小程序服务
     */
    @Bean
    public WxMaService wxMaService() {
        return new WxMaServiceImpl();
    }

    /**
     * 微信小程序二维码服务
     */
    @Bean
    public WxMaQrcodeService wxMaQrcodeService(WxMaService wxMaService) {
        return wxMaService.getQrcodeService();
    }

    /**
     * 微信配置管理器
     */
    @Bean
    public WxMaConfigManager wxMaConfigManager(WxMaService wxMaService) {
        return new WxMaConfigManager(wxMaService, remotePmsSocialDetailsService);
    }

    @Bean
    public WxMaServiceHelper wxMaServiceHelper(WxMaService wxMaService, SocialWechatProperties properties) {
        return new WxMaServiceHelper(wxMaService, properties);
    }

    // ==================== 事件监听器 ====================

    /**
     * 配置变更事件监听器
     */
    @Bean
    public SocialConfigChangedListener socialConfigChangedListener(WxMaConfigManager wxMaConfigManager) {
        return new SocialConfigChangedListener(wxMaConfigManager);
    }

    // ==================== API接口 ====================

    /**
     * 配置刷新管理API
     */
    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public WxMaConfigRefreshAPI wxMaConfigRefreshAPI(
            WxMaConfigManager wxMaConfigManager,
            SocialConfigMessagePublisher configMessagePublisher) {
        return new WxMaConfigRefreshAPI(wxMaConfigManager, configMessagePublisher);
    }

    // ==================== 消息发布器（仅PMS服务） ====================

    /**
     * Redis消息发布器
     * 条件：1. 启用发布功能 2. 使用Redis作为消息队列 3. Redis可用
     */
    @Bean
    @ConditionalOnProperty(prefix = "ingot.social", name = "message-queue", havingValue = "redis", matchIfMissing = true)
    @ConditionalOnBean(StringRedisTemplate.class)
    public SocialConfigMessagePublisher socialConfigMessagePublisher(
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper) {
        String topic = socialConfigProperties.getRedis().getTopic();
        log.info("WechatConfiguration - 初始化Redis消息发布器，主题: {}", topic);
        return new RedisSocialConfigMessagePublisher(stringRedisTemplate, objectMapper, topic);
    }

    // Kafka消息发布器（扩展）
    // @Bean
    // @ConditionalOnProperty(prefix = "ingot.social", name = "message-queue", havingValue = "kafka")
    // @ConditionalOnBean(KafkaTemplate.class)
    // public SocialConfigMessagePublisher kafkaSocialConfigMessagePublisher(KafkaTemplate kafkaTemplate) {
    //     return new KafkaSocialConfigMessagePublisher(kafkaTemplate);
    // }

    // ==================== Redis消息监听器 ====================

    /**
     * Redis消息监听器
     * 条件：使用Redis作为消息队列且Redis可用
     */
    @Bean
    @ConditionalOnProperty(prefix = "ingot.social", name = "message-queue", havingValue = "redis", matchIfMissing = true)
    @ConditionalOnBean(RedisConnectionFactory.class)
    public SocialConfigRedisMessageListener socialConfigRedisMessageListener(
            ApplicationEventPublisher eventPublisher,
            ObjectMapper objectMapper) {
        return new SocialConfigRedisMessageListener(eventPublisher, objectMapper);
    }

    /**
     * Redis消息监听容器
     * 条件：使用Redis作为消息队列且Redis可用
     */
    @Bean
    @ConditionalOnProperty(prefix = "ingot.social", name = "message-queue", havingValue = "redis", matchIfMissing = true)
    @ConditionalOnBean(RedisConnectionFactory.class)
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            SocialConfigRedisMessageListener messageListener) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        String topic = socialConfigProperties.getRedis().getTopic();
        container.addMessageListener(messageListener, new ChannelTopic(topic));

        log.info("WechatConfiguration - Redis消息监听器已配置，主题: {}", topic);
        return container;
    }

    // Kafka消息监听器（扩展）
    // @Bean
    // @ConditionalOnProperty(prefix = "ingot.social", name = "message-queue", havingValue = "kafka")
    // public MessageListenerAdapter kafkaMessageListener() {
    //     // Kafka监听器实现
    // }

    // ==================== 初始化 ====================

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("WechatConfiguration - 开始初始化微信配置...");
        log.info("WechatConfiguration - 消息队列类型: {}", socialConfigProperties.getMessageQueue());
    }
}
