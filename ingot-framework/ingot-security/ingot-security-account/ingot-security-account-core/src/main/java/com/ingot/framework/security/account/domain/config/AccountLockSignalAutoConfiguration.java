package com.ingot.framework.security.account.domain.config;

import com.ingot.framework.security.account.domain.port.outbound.AccountLockSignalPort;
import com.ingot.framework.security.account.domain.port.outbound.redis.RedisAccountLockSignalAdapter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * <p>在存在 {@link StringRedisTemplate} 时注册 Redis 锁定信号 Port。</p>
 *
 * <p>与账号用例装配解耦，BFF / Auth 等只需读信号的进程可单独启用本配置。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnClass(StringRedisTemplate.class)
@EnableConfigurationProperties(AccountLockSignalProperties.class)
public class AccountLockSignalAutoConfiguration {

    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    @ConditionalOnMissingBean(AccountLockSignalPort.class)
    public AccountLockSignalPort redisAccountLockSignalPort(
            StringRedisTemplate stringRedisTemplate,
            AccountLockSignalProperties properties) {
        return new RedisAccountLockSignalAdapter(stringRedisTemplate, properties);
    }
}
