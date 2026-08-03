package com.ingot.framework.cache.coordinator;

/**
 * <p>缓存刷新回调，在共享缓存从下游取回新值时被通知。</p>
 *
 * <p>供「不在请求路径上读缓存」的组件感知数据更新，典型如网关的 Sentinel 规则重载。
 * 实现应自行做版本比对以保证幂等——同一份数据可能被多次通知。</p>
 *
 * @param <V> 缓存值类型
 * @author jy
 * @since 1.0.0
 * @see CacheRefreshPublisher
 */
@FunctionalInterface
public interface CacheRefreshListener<V> {

    /**
     * 处理一次缓存刷新。
     *
     * @param value 本次取回的值
     */
    void onRefresh(V value);
}
