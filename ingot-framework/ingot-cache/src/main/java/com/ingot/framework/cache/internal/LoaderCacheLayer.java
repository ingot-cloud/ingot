package com.ingot.framework.cache.internal;

import java.util.function.Supplier;

import com.ingot.framework.cache.spi.CacheValueLoader;
import com.ingot.framework.cache.spi.LayeredCache;
import com.ingot.framework.cache.source.CacheSource;
import com.ingot.framework.cache.source.CacheSourceHolder;

/**
 * <p>把 {@link CacheValueLoader} 适配为装饰器链最内层的 {@link LayeredCache}，自身不做任何缓存。</p>
 *
 * <p>当弹性降级关闭时由本层直接承接上层的 miss 穿透，
 * {@link com.ingot.framework.cache.spi.RemoteUnavailableException} 原样上抛，不写 LKG 也不落地板。
 * 关闭弹性只是移除降级能力，链路结构保持完整，不会因少一层而导致上层无法装配。</p>
 *
 * @param <K> 缓存键类型
 * @param <V> 缓存值类型
 * @author jy
 * @since 1.0.0
 * @see ResilientCacheLayer
 */
public class LoaderCacheLayer<K, V> implements LayeredCache<K, V> {

    private final String name;
    private final CacheValueLoader<K, V> loader;
    private final Supplier<V> emptyValue;
    private final CacheSourceHolder sourceHolder;

    public LoaderCacheLayer(String name,
                            CacheValueLoader<K, V> loader,
                            Supplier<V> emptyValue,
                            CacheSourceHolder sourceHolder) {
        this.name = name;
        this.loader = loader;
        this.emptyValue = emptyValue;
        this.sourceHolder = sourceHolder;
    }

    @Override
    public V get(K key) {
        V data = loader.load(key);
        if (data == null && emptyValue != null) {
            data = emptyValue.get();
        }
        if (sourceHolder != null) {
            sourceHolder.mark(CacheSource.REMOTE);
        }
        return data;
    }

    @Override
    public void evict(K key) {
        // 无缓存可清
    }

    @Override
    public void evictAll() {
        // 无缓存可清
    }

    @Override
    public String name() {
        return name;
    }
}
