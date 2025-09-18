package com.ingot.framework.data.redis.service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.ingot.framework.data.redis.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * <p>Description  : DefaultRedisCacheService.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/3/7.</p>
 * <p>Time         : 14:56.</p>
 */
@RequiredArgsConstructor
public class DefaultRedisCacheService implements RedisCacheService {
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void cache(String key, Object value) {
        redisTemplate.opsForValue().set(RedisUtils.getCacheName(key), value);
    }

    @Override
    public void cache(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(RedisUtils.getCacheName(key), value, timeout, unit);
    }

    @Override
    public <T> T get(String key) {
        Object target = redisTemplate.opsForValue().get(RedisUtils.getCacheName(key));
        if (target == null) {
            return null;
        }
        try {
            return (T) target;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(RedisUtils.getCacheName(key));
    }

    @Override
    public void delete(List<String> patterns) {
        RedisUtils.deleteKeys(redisTemplate, patterns);
    }
}
