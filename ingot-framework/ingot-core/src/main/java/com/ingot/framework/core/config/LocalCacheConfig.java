package com.ingot.framework.core.config;

import java.time.Duration;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;

/**
 * LocalCacheConfig
 *
 * @author jy
 * @since 2026/2/11
 */
@AutoConfiguration
@EnableConfigurationProperties(LocalCacheProperties.class)
public class LocalCacheConfig {
    public static final String CACHE_MANAGER = "localRuntimeCacheManager";

    @Bean(CACHE_MANAGER)
    @ConditionalOnMissingBean(name = CACHE_MANAGER)
    public CacheManager policyRuntimeCacheManager(LocalCacheProperties properties) {
        CaffeineCacheManager cm = new CaffeineCacheManager();
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder();
        caffeine.maximumSize(properties.getMaximumSize());
        if (properties.getExpireAfterWrite() != -1) {
            caffeine.expireAfterWrite(Duration.ofMinutes(properties.getExpireAfterWrite()));
        }
        cm.setCaffeine(caffeine);
        return cm;
    }
}
