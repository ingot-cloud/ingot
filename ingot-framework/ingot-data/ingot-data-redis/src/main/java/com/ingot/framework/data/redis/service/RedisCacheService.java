package com.ingot.framework.data.redis.service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>Description  : RedisCacheService.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/3/7.</p>
 * <p>Time         : 14:51.</p>
 */
public interface RedisCacheService {

    /**
     * 缓存
     *
     * @param key   key
     * @param value value
     */
    void cache(String key, Object value);

    /**
     * 缓存
     *
     * @param key     key
     * @param value   value
     * @param timeout 超时时间
     * @param unit    超时时间单位
     */
    void cache(String key, Object value, long timeout, TimeUnit unit);

    /**
     * 获取缓存
     *
     * @param key key
     * @param <T> 泛型
     * @return 指定对象
     */
    <T> T get(String key);

    /**
     * 删除缓存
     *
     * @param key key
     */
    void delete(String key);

    /**
     * 删除缓存
     *
     * @param patterns pattern
     */
    void delete(List<String> patterns);
}
