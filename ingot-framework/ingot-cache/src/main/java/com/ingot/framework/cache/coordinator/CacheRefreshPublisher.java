package com.ingot.framework.cache.coordinator;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>缓存刷新事件的注册与分发中心，解耦缓存构建时机与监听方装配时机。</p>
 *
 * <p>缓存实例通常在框架自动配置阶段构建，而监听方（如网关的 Sentinel 配置）在业务模块中装配，
 * 两者无法在同一处接线。发布器作为共享中介先行创建，双方各自持有引用即可。</p>
 *
 * <p>单个监听器抛出异常只记录日志，不影响其余监听器，也不会传播回触发刷新的读请求。</p>
 *
 * @param <V> 缓存值类型
 * @author jy
 * @since 1.0.0
 * @see CacheRefreshListener
 */
@Slf4j
public class CacheRefreshPublisher<V> {

    private final String name;
    private final List<CacheRefreshListener<V>> listeners = new CopyOnWriteArrayList<>();

    public CacheRefreshPublisher(String name) {
        this.name = name;
    }

    /**
     * 追加一个刷新监听器。
     *
     * @param listener 监听器
     */
    public void addListener(CacheRefreshListener<V> listener) {
        if (listener != null) {
            listeners.add(listener);
            log.info("[Cache:{}] refresh listener registered, total={}", name, listeners.size());
        }
    }

    /**
     * 广播一次刷新；无监听器时为空操作。
     *
     * @param value 本次取回的值
     */
    public void publish(V value) {
        if (listeners.isEmpty()) {
            return;
        }
        for (CacheRefreshListener<V> listener : listeners) {
            try {
                listener.onRefresh(value);
            } catch (Exception e) {
                log.warn("[Cache:{}] refresh listener failed", name, e);
            }
        }
    }
}
