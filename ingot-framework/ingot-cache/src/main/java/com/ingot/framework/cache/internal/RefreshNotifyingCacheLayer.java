package com.ingot.framework.cache.internal;

import java.util.concurrent.atomic.AtomicReference;

import com.ingot.framework.cache.coordinator.CacheRefreshPublisher;
import com.ingot.framework.cache.spi.LayeredCache;

/**
 * <p>装饰器链最外层的通知层，在缓存值发生变化时广播一次刷新事件。</p>
 *
 * <p>存在的意义是让「没有请求路径读者」的消费者也能感知刷新。典型场景是网关限流：Sentinel 读的是自己
 * 运行时已加载的规则，不碰缓存，因此单靠 TTL 刷新缓存并不会让规则更新；订阅本层的刷新事件，
 * 才能在 TTL 懒刷新路径上触发规则重载。</p>
 *
 * <p>位置必须在最外层而不是 L1 之下：监听器回调里通常会回读缓存（例如取转换后的领域模型），
 * 若此时 L1 尚未写入，回读就会再次穿透到远端，把一次加载放大成两次。放在最外层则回读必然命中 L1。</p>
 *
 * <p>用引用比对而非每次都发：只有下游返回了与上次不同的对象才广播，因此 L1 命中期间完全静默，
 * 监听器回调内的回读也不会触发二次广播，从根本上排除递归。</p>
 *
 * @param <K> 缓存键类型
 * @param <V> 缓存值类型
 * @author jy
 * @since 1.0.0
 * @see CacheRefreshPublisher
 * @apiNote 引用比对不区分缓存键，仅适用于单 key 的共享快照场景；多 key 缓存不应挂载刷新发布器。
 */
public class RefreshNotifyingCacheLayer<K, V> implements LayeredCache<K, V> {

    private final String name;
    private final LayeredCache<K, V> delegate;
    private final CacheRefreshPublisher<V> publisher;
    private final AtomicReference<V> lastPublished = new AtomicReference<>();

    public RefreshNotifyingCacheLayer(String name,
                                      LayeredCache<K, V> delegate,
                                      CacheRefreshPublisher<V> publisher) {
        this.name = name;
        this.delegate = delegate;
        this.publisher = publisher;
    }

    @Override
    public V get(K key) {
        V value = delegate.get(key);
        V previous = lastPublished.get();
        if (previous != value && lastPublished.compareAndSet(previous, value)) {
            publisher.publish(value);
        }
        return value;
    }

    @Override
    public void evict(K key) {
        delegate.evict(key);
    }

    @Override
    public void evictAll() {
        delegate.evictAll();
    }

    @Override
    public String name() {
        return name;
    }
}
