package com.ingot.framework.id.config;

import com.baomidou.mybatisplus.autoconfigure.IdentifierGeneratorAutoConfiguration;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.ingot.framework.id.BizGenerator;
import com.ingot.framework.id.IdGenerator;
import com.ingot.framework.id.leaf.DefaultBizGenerator;
import com.ingot.framework.id.mybatis.InIdentifierGenerator;
import com.ingot.framework.id.properties.IdProperties;
import com.ingot.framework.id.snowflake.impl.SnowFlakeIdGenerator;
import com.ingot.framework.id.snowflake.worker.WorkerIdFactory;
import com.ingot.framework.id.snowflake.worker.impl.MachineWorkerIdFactory;
import com.ingot.framework.id.snowflake.worker.impl.RedisWorkerIdFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;

import javax.sql.DataSource;

/**
 * <p>Description  : IdAutoConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/10/25.</p>
 * <p>Time         : 3:30 下午.</p>
 */
@AutoConfiguration
@AutoConfigureBefore(IdentifierGeneratorAutoConfiguration.class)
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
@EnableConfigurationProperties(value = IdProperties.class)
public class IdAutoConfig {
    @Value("${spring.application.name}")
    private String serverName;
    @Value("${server.port}")
    private int port;

    @Bean
    @ConditionalOnMissingBean(IdGenerator.class)
    @ConditionalOnProperty(value = "ingot.id.mode", havingValue = "redis")
    @ConditionalOnBean(RedisTemplate.class)
    public IdGenerator redisIdGenerator(RedisTemplate<String, Object> redisTemplate,
                                        IdProperties properties) {
        WorkerIdFactory factory = new RedisWorkerIdFactory(properties.getLocalPathPrefix(),
                serverName, String.valueOf(port), redisTemplate);
        return new SnowFlakeIdGenerator(factory);
    }

    @Bean
    @ConditionalOnMissingBean(IdGenerator.class)
    @ConditionalOnProperty(value = "ingot.id.mode", havingValue = "machine", matchIfMissing = true)
    public IdGenerator machineIdGenerator(IdProperties properties) {
        WorkerIdFactory factory = new MachineWorkerIdFactory(properties,
                serverName, String.valueOf(port));
        return new SnowFlakeIdGenerator(factory);
    }

    @Bean
    @ConditionalOnMissingBean(BizGenerator.class)
    @ConditionalOnBean(DataSource.class)
    public BizGenerator bizGenerator(DataSource dataSource) {
        return new DefaultBizGenerator(dataSource);
    }

    /**
     * 需要在 {@link IdentifierGeneratorAutoConfiguration} 前配置
     */
    @Bean
    @ConditionalOnBean(IdGenerator.class)
    public IdentifierGenerator customIdentifierGenerator(IdGenerator idGenerator) {
        return new InIdentifierGenerator(idGenerator);
    }
}
