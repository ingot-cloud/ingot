package com.ingot.framework.cache.internal;

import java.time.Duration;
import java.util.function.Predicate;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ingot.framework.cache.spi.LayeredCache;

/**
 * <p>L1 进程内 Caffeine 缓存层，吸收绝大部分读请求，并以 TTL 为跨节点一致性提供最后兜底。</p>
 *
 * <p>TTL 不是可选优化：失效广播走 Redis Pub/Sub，无持久化也无重投，消息丢失的节点若没有 TTL
 * 会永久持有旧数据。有了 TTL，最迟一个周期内该节点自动收敛。</p>
 *
 * <p>与 L2 一致地不缓存「不合格」的值（由 {@code cacheable} 判定），读到时主动 invalidate 并穿透。</p>
 *
 * @param <K> 缓存键类型
 * @param <V> 缓存值类型
 * @author jy
 * @since 1.0.0
 */
public class CaffeineCacheLayer<K, V> implements LayeredCache<K, V> {

    private final String name;
    private final LayeredCache<K, V> delegate;
    private final Cache<K, V> cache;
    private final Predicate<V> cacheable;

    public CaffeineCacheLayer(String name,
                              LayeredCache<K, V> delegate,
                              Duration ttl,
                              long maximumSize,
                              Predicate<V> cacheable) {
        this.name = name;
        this.delegate = delegate;
        this.cacheable = cacheable;
        this.cache = Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .expireAfterWrite(ttl)
                .build();
    }

    @Override
    public V get(K key) {
        V hit = cache.getIfPresent(key);
        if (hit != null) {
            if (cacheable.test(hit)) {
                return hit;
            }
            cache.invalidate(key);
        }
        V fresh = delegate.get(key);
        if (cacheable.test(fresh)) {
            cache.put(key, fresh);
        }
        return fresh;
    }

    @Override
    public void evict(K key) {
        cache.invalidate(key);
        delegate.evict(key);
    }

    @Override
    public void evictAll() {
        cache.invalidateAll();
        delegate.evictAll();
    }

    @Override
    public String name() {
        return name;
    }
}
