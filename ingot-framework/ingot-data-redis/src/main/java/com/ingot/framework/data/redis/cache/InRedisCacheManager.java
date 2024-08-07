package com.ingot.framework.data.redis.cache;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.data.redis.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.time.Duration;
import java.util.Map;

/**
 * <p>Description  : IngotRedisCacheManager.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/10/19.</p>
 * <p>Time         : 6:18 下午.</p>
 */
@Slf4j
public class InRedisCacheManager extends RedisCacheManager {
    private static final String SPLIT_FLAG = "#";
    private static final int CACHE_LENGTH = 2;

    public InRedisCacheManager(RedisCacheWriter cacheWriter,
                               RedisCacheConfiguration defaultCacheConfiguration,
                               boolean allowInFlightCacheCreation,
                               Map<String, RedisCacheConfiguration> initialCacheConfigurations) {
        super(cacheWriter, defaultCacheConfiguration, allowInFlightCacheCreation, initialCacheConfigurations);
    }

    @Override
    @NonNull
    protected RedisCache createRedisCache(@NonNull String name,
                                          @Nullable RedisCacheConfiguration cacheConfig) {
        if (StrUtil.isBlank(name) || !name.contains(SPLIT_FLAG)) {
            return super.createRedisCache(name, cacheConfig);
        }

        String[] cacheArray = name.split(SPLIT_FLAG);
        if (cacheArray.length < CACHE_LENGTH) {
            return super.createRedisCache(name, cacheConfig);
        }

        if (cacheConfig != null) {
            long cacheAge = Long.parseLong(cacheArray[1]);
            cacheConfig = cacheConfig.entryTtl(Duration.ofSeconds(cacheAge));
        }
        return super.createRedisCache(cacheArray[0], cacheConfig);
    }

    /**
     * 从上下文中获取租户ID，重写@Cacheable value 值
     */
    @Override
    public Cache getCache(@NonNull String name) {
        String finalCacheName = RedisUtils.getCacheName(name);
        log.info("IngotRedisCacheManager - getCache name={}, finalCacheName={}", name, finalCacheName);
        return super.getCache(finalCacheName);
    }
}