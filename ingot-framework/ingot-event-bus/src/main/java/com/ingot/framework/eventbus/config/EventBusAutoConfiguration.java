package com.ingot.framework.eventbus.config;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.eventbus.InvalidationBus;
import com.ingot.framework.eventbus.redis.RedisInvalidationBus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * 事件总线自动配置。默认启用 Redis 实现，可通过 {@code ingot.event-bus.type=none} 关闭。
 * <p>
 * 必须排在 {@code ingot-data-redis} 的 {@code InRedisTemplateConfiguration} /
 * {@code InRedisMessageConfiguration} <strong>之后</strong>：本类的
 * {@code InvalidationBus} bean 条件依赖 {@link StringRedisTemplate} 与
 * {@link RedisMessageListenerContainer} 已注册。
 * 若仅写 {@code @AutoConfigureAfter(RedisAutoConfiguration.class)}，在拓扑排序下仍可能
 * 早于上述 Configuration 执行，导致 {@code @ConditionalOnBean(RedisMessageListenerContainer)}
 * 判定失败、总线永不装配，进而跨节点字典失效广播整条链路静默失效（表现为只有写端 PMS
 * 本地缓存被清，其它微服务 L1 永远不过期）。
 * </p>
 *
 * @author jy
 * @since 2026/4/27
 */
@Slf4j
@AutoConfiguration
@AutoConfigureAfter(value = RedisAutoConfiguration.class, name = {
        "com.ingot.framework.data.redis.config.InRedisTemplateConfiguration",
        "com.ingot.framework.data.redis.config.InRedisMessageConfiguration"
})
@ConditionalOnClass({RedisTemplate.class, RedisMessageListenerContainer.class})
@ConditionalOnProperty(value = "ingot.event-bus.type", havingValue = "redis", matchIfMissing = true)
@EnableConfigurationProperties(EventBusProperties.class)
public class EventBusAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(InvalidationBus.class)
    @ConditionalOnBean({StringRedisTemplate.class, RedisMessageListenerContainer.class})
    public InvalidationBus invalidationBus(StringRedisTemplate redisTemplate,
                                           RedisMessageListenerContainer listenerContainer,
                                           ObjectProvider<ObjectMapper> objectMapperProvider,
                                           EventBusProperties properties,
                                           Environment env) {
        ObjectMapper mapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
        String origin = properties.getOrigin();
        if (origin == null || origin.isBlank()) {
            String app = env.getProperty("spring.application.name", "ingot");
            origin = app + ":" + UUID.randomUUID();
            properties.setOrigin(origin);
        }
        log.info("[EventBus] initialized RedisInvalidationBus origin={}, topicPrefix={}",
                origin, properties.getRedis().getTopicPrefix());
        return new RedisInvalidationBus(redisTemplate, listenerContainer, mapper, properties, origin);
    }
}
