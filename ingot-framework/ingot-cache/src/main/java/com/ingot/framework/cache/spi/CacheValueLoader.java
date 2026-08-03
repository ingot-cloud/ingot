package com.ingot.framework.cache.spi;

/**
 * <p>分层缓存链最内层的真实数据加载器，通常是 Feign 远端调用或本地 DB 查询。</p>
 *
 * <p>实现必须区分两种「无数据」语义：远端调用成功但结果为空是<b>合法空</b>，直接返回空值即可，
 * 会被视为有效数据用于刷新 LKG；只有远端确实不可用（超时、连接失败、业务失败码）时才抛出
 * {@link RemoteUnavailableException}，触发降级阶梯。混淆两者会导致 LKG 被空数据污染。</p>
 *
 * @param <K> 缓存键类型
 * @param <V> 缓存值类型
 * @author jy
 * @since 1.0.0
 * @see RemoteUnavailableException
 */
@FunctionalInterface
public interface CacheValueLoader<K, V> {

    /**
     * 加载指定键的真实数据。
     *
     * @param key 缓存键
     * @return 加载结果；{@code null} 与空集合均表示合法空
     * @throws RemoteUnavailableException 数据源不可用
     */
    V load(K key);
}
