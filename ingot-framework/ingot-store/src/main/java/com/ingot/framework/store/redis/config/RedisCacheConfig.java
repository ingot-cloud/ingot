package com.ingot.framework.store.redis.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizer;
import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizers;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * <p>Description  : RedisCacheConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/10/21.</p>
 * <p>Time         : 6:00 下午.</p>
 */
@AutoConfiguration
@ConditionalOnMissingBean(CacheManagerCustomizers.class)
public class RedisCacheConfig {

    @Bean
    public CacheManagerCustomizers cacheManagerCustomizers(ObjectProvider<List<CacheManagerCustomizer<?>>> customizers) {
        return new CacheManagerCustomizers(customizers.getIfAvailable());
    }
}
