package com.ingot.framework.data.redis.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * <p>Description  : InRedisMessageConfiguration.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2024/6/29.</p>
 * <p>Time         : 08:52.</p>
 */
@Configuration(proxyBeanMethods = false)
public class InRedisMessageConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RedisMessageListenerContainer redisContainer(RedisConnectionFactory redisConnectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        return container;
    }

}
