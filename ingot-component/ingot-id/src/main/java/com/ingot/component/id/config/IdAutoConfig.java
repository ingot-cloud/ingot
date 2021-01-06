package com.ingot.component.id.config;

import com.ingot.component.id.IdGenerator;
import com.ingot.component.id.impl.SnowFlakeIdGenerator;
import com.ingot.component.id.properties.IdProperties;
import com.ingot.component.id.worker.WorkerIdFactory;
import com.ingot.component.id.worker.impl.MachineWorkerIdFactory;
import com.ingot.component.id.worker.impl.RedisWorkerIdFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
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
    @ConditionalOnBean(RedisTemplate.class)
    public IdGenerator redisIdGenerator(RedisTemplate<String, Object> redisTemplate,
                                        IdProperties properties) {
        WorkerIdFactory factory = new RedisWorkerIdFactory(properties.getLocalPathPrefix(),
                serverName, String.valueOf(port), redisTemplate);
        return new SnowFlakeIdGenerator(factory);
    }

    @Bean("idGenerator")
    @ConditionalOnMissingBean(IdGenerator.class)
    @ConditionalOnProperty(value = "ingot.id.mode", havingValue = "machine", matchIfMissing = true)
    public IdGenerator machineIdGenerator(IdProperties properties){
        WorkerIdFactory factory = new MachineWorkerIdFactory(properties.getLocalPathPrefix(),
                serverName, String.valueOf(port));
        return new SnowFlakeIdGenerator(factory);
    }
}
