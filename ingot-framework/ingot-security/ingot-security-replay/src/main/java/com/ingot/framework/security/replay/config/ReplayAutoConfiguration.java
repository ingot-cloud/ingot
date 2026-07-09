package com.ingot.framework.security.replay.config;

import com.ingot.framework.security.replay.DefaultReplayGuard;
import com.ingot.framework.security.replay.ReplayGuard;
import com.ingot.framework.security.replay.ReplayProperties;
import com.ingot.framework.security.replay.idempotent.IdempotentAspect;
import com.ingot.framework.security.replay.store.NonceStore;
import com.ingot.framework.security.replay.store.RedisNonceStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * <p>防重放能力的自动配置。</p>
 *
 * <p>在存在 {@link StringRedisTemplate} 时装配 nonce 存储、防重放校验器与幂等切面，
 * 全部以 {@link ConditionalOnMissingBean} 声明，允许业务侧自定义替换。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnClass(StringRedisTemplate.class)
@EnableConfigurationProperties(ReplayProperties.class)
public class ReplayAutoConfiguration {

    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    @ConditionalOnMissingBean(NonceStore.class)
    public NonceStore redisNonceStore(StringRedisTemplate stringRedisTemplate) {
        return new RedisNonceStore(stringRedisTemplate);
    }

    @Bean
    @ConditionalOnBean(NonceStore.class)
    @ConditionalOnMissingBean(ReplayGuard.class)
    public ReplayGuard replayGuard(ReplayProperties properties, NonceStore nonceStore) {
        return new DefaultReplayGuard(properties, nonceStore);
    }

    @Bean
    @ConditionalOnBean(NonceStore.class)
    @ConditionalOnMissingBean(IdempotentAspect.class)
    public IdempotentAspect idempotentAspect(NonceStore nonceStore, ReplayProperties properties) {
        return new IdempotentAspect(nonceStore, properties);
    }
}
