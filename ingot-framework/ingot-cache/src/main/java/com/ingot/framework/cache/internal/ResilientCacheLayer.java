package com.ingot.framework.cache.internal;

import java.util.function.Supplier;

import com.ingot.framework.cache.source.CacheSource;
import com.ingot.framework.cache.source.CacheSourceHolder;
import com.ingot.framework.cache.spi.CacheFloorSupplier;
import com.ingot.framework.cache.spi.CacheValueLoader;
import com.ingot.framework.cache.spi.LayeredCache;
import com.ingot.framework.cache.spi.RemoteUnavailableException;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>弹性降级层，按 {@code remote → LKG → 本地地板} 阶梯兜底，任何一级都不 fail-open。</p>
 *
 * <p>本层位于装饰器链<b>最内侧</b>（L1/L2 之下）。这个位置是刻意的：降级值会被上层热缓存按 TTL 持有，
 * TTL 到期后自动重试远端，故障恢复无需依赖失效广播。反过来若把本层放在最外层，降级值将不受 TTL 约束，
 * 远端恢复后仍会长期返回旧兜底数据。</p>
 *
 * <p>远端成功（含合法空）时刷新 LKG 并标记 {@link CacheSource#REMOTE}；抛出
 * {@link RemoteUnavailableException} 时依次尝试 LKG 与地板；地板被禁用且无 LKG 时原样上抛，
 * 由调用方 fail-closed，绝不返回「失败空」。</p>
 *
 * @param <K> 缓存键类型
 * @param <V> 缓存值类型
 * @author jy
 * @since 1.0.0
 * @see LastKnownGoodStore
 * @see CacheFloorSupplier
 */
@Slf4j
public class ResilientCacheLayer<K, V> implements LayeredCache<K, V> {

    private final String name;
    private final CacheValueLoader<K, V> loader;
    private final LastKnownGoodStore<K, V> lkgStore;
    private final CacheFloorSupplier<K, V> floorSupplier;
    private final boolean localFloorEnabled;
    private final Supplier<V> emptyValue;
    private final CacheSourceHolder sourceHolder;

    public ResilientCacheLayer(String name,
                               CacheValueLoader<K, V> loader,
                               LastKnownGoodStore<K, V> lkgStore,
                               CacheFloorSupplier<K, V> floorSupplier,
                               boolean localFloorEnabled,
                               Supplier<V> emptyValue,
                               CacheSourceHolder sourceHolder) {
        this.name = name;
        this.loader = loader;
        this.lkgStore = lkgStore;
        this.floorSupplier = floorSupplier;
        this.localFloorEnabled = localFloorEnabled;
        this.emptyValue = emptyValue;
        this.sourceHolder = sourceHolder;
    }

    @Override
    public V get(K key) {
        try {
            V data = loader.load(key);
            if (data == null && emptyValue != null) {
                data = emptyValue.get();
            }
            if (lkgStore != null) {
                lkgStore.save(key, data);
            }
            mark(CacheSource.REMOTE);
            return data;
        } catch (RemoteUnavailableException e) {
            return fallback(key, e);
        }
    }

    private V fallback(K key, RemoteUnavailableException cause) {
        V lkg = lkgStore == null ? null : lkgStore.load(key);
        if (lkg != null) {
            mark(CacheSource.LAST_KNOWN_GOOD);
            log.warn("[Cache:{}] remote unavailable, using LKG, cause={}", name, cause.getMessage());
            return lkg;
        }
        if (!localFloorEnabled || floorSupplier == null) {
            log.error("[Cache:{}] remote unavailable, no LKG, floor disabled, fail closed", name, cause);
            throw cause;
        }
        V floor = floorSupplier.get(key);
        mark(CacheSource.LOCAL_FLOOR);
        log.warn("[Cache:{}] remote unavailable, no LKG, using local floor, cause={}", name, cause.getMessage());
        return floor;
    }

    private void mark(CacheSource source) {
        if (sourceHolder != null) {
            sourceHolder.mark(source);
        }
    }

    @Override
    public void evict(K key) {
        // 仅热缓存参与失效；LKG 生命周期独立，不随失效事件清除
    }

    @Override
    public void evictAll() {
        // 同 evict(K)：不清 LKG
    }

    @Override
    public String name() {
        return name;
    }
}
