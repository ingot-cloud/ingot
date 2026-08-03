package com.ingot.framework.cache.spi;

/**
 * <p>降级阶梯最后一级的本地地板供给器，通常读取 Nacos 下发的兜底配置。</p>
 *
 * <p>仅在远端不可用<b>且</b>无 LKG 时被调用。实现必须保证返回非空的有效值——地板的存在意义就是
 * 避免 fail-open 到「无策略」，若地板本身也可能为空，应在实现内补上最小基线。</p>
 *
 * @param <K> 缓存键类型；单 key 场景可忽略该参数
 * @param <V> 缓存值类型
 * @author jy
 * @since 1.0.0
 */
@FunctionalInterface
public interface CacheFloorSupplier<K, V> {

    /**
     * 提供指定键的地板兜底值。
     *
     * @param key 缓存键
     * @return 非空兜底值
     */
    V get(K key);
}
