package com.ingot.framework.cache.spi;

/**
 * <p>分层缓存的统一读写口，每一层实现都是持有下层引用的装饰器。</p>
 *
 * <p>典型链路自外向内为 {@code L1 Caffeine → L2 Redis → Resilient(remote → LKG → 地板)}，
 * 由 {@code LayeredCacheBuilder} 装配。{@link #get(Object)} 逐层向下穿透并回填，
 * {@link #evict(Object)} 与 {@link #evictAll()} 逐层向下清理热缓存，
 * 但不会触及 LKG——后者生命周期独立于失效事件。</p>
 *
 * <p>单 key 场景（如策略全量快照）传固定常量 key，多 key 场景（如字典项）传业务 key，
 * 两者共用同一套装饰器。</p>
 *
 * @param <K> 缓存键类型
 * @param <V> 缓存值类型
 * @author jy
 * @since 1.0.0
 * @apiNote 实现必须线程安全。{@link #get(Object)} 在最内层远端不可用且无任何兜底时向上抛
 *          {@link RemoteUnavailableException}，调用方需自行决定是 fail-closed 还是补默认值。
 */
public interface LayeredCache<K, V> {

    /**
     * 读取缓存值，miss 时逐层向下加载并回填。
     *
     * @param key 缓存键
     * @return 缓存值；远端返回「合法空」时可能为空集合或空对象
     * @throws RemoteUnavailableException 远端不可用且无 LKG、地板兜底
     */
    V get(K key);

    /**
     * 清除单个键的热缓存（L1 + L2），不影响 LKG。
     *
     * @param key 缓存键
     */
    void evict(K key);

    /**
     * 清除全部热缓存（L1 + L2），不影响 LKG。
     */
    void evictAll();

    /**
     * 缓存实例名，用于日志与 Actuator 汇总展示。
     *
     * @return 实例名
     */
    String name();
}
