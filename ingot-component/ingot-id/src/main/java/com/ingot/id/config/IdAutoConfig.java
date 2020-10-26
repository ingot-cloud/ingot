package com.ingot.id.config;

import com.ingot.id.IdGenerator;
import com.ingot.id.impl.SnowFlakeIdGenerator;
import com.ingot.id.properties.IdProperties;
import com.ingot.id.worker.impl.RedisWorkerIdFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * <p>Description  : IdAutoConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/10/25.</p>
 * <p>Time         : 3:30 下午.</p>
 */
@Configuration
@EnableConfigurationProperties(value = IdProperties.class)
public class IdAutoConfig {
    @Value("${spring.application.name}")
    private String serverName;
    @Value("${server.port}")
    private int port;

    @Bean("idGenerator")
    @ConditionalOnMissingBean(IdGenerator.class)
    @ConditionalOnProperty(value = "ingot.id.mode", havingValue = "redis")
    public IdGenerator redisIdGenerator(RedisTemplate<String, Object> redisTemplate,
                                        IdProperties properties) {
        RedisWorkerIdFactory factory = new RedisWorkerIdFactory(properties.getLocalPathPrefix(),
                serverName, String.valueOf(port), redisTemplate);
        return new SnowFlakeIdGenerator(factory);
    }
}
