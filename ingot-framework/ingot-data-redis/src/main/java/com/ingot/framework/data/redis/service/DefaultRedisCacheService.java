package com.ingot.framework.data.redis.service;

import com.ingot.framework.data.redis.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.concurrent.TimeUnit;

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
        redisTemplate.opsForValue().set(key, value);
    }

    @Override
    public void cache(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    @Override
    public <T> T get(String key) {
        Object target = redisTemplate.opsForValue().get(key);
        if (target == null) {
            return null;
        }
        return (T) target;
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public void delete(List<String> patterns) {
        RedisUtils.deleteKeys(redisTemplate, patterns);
    }
}
