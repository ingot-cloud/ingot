package com.ingot.framework.data.redis.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ingot.framework.commons.jackson.InJavaTimeModule;
import com.ingot.framework.commons.jackson.InModule;
import com.ingot.framework.data.redis.service.DefaultRedisCacheService;
import com.ingot.framework.data.redis.service.RedisCacheService;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import static com.ingot.framework.commons.constants.BeanIds.REDIS_TEMPLATE;

/**
 * <p>Description  : RedisTemplate配置.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/10/19.</p>
 * <p>Time         : 5:47 下午.</p>
 */
@EnableCaching
@Configuration
@AutoConfigureBefore(name = { "org.redisson.spring.starter.RedissonAutoConfigurationV2",
        "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration" })
public class InRedisTemplateConfiguration {

    @Bean
    public StringRedisSerializer stringRedisSerializer() {
        return new StringRedisSerializer();
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(factory);
        return template;
    }

    @Bean(REDIS_TEMPLATE)
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();

        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.activateDefaultTyping(om.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.WRAPPER_ARRAY);
        om.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        om.registerModules(new InModule(), new JavaTimeModule(), new InJavaTimeModule());

        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer =
                new Jackson2JsonRedisSerializer<>(om, Object.class);

        template.setKeySerializer(stringRedisSerializer());
        template.setHashKeySerializer(stringRedisSerializer());
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);

        template.setConnectionFactory(factory);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisCacheService redisCacheService(RedisTemplate<String, Object> redisTemplate) {
        return new DefaultRedisCacheService(redisTemplate);
    }
}
