package com.ingot.framework.cache.registry;

import com.ingot.framework.cache.source.CacheSourceHolder;

/**
 * <p>单个分层缓存实例的装配画像，供 Actuator 汇总展示各缓存的层次开关与降级态。</p>
 *
 * @param name              缓存实例名
 * @param l1Enabled         是否启用 L1
 * @param l2Enabled         是否启用 L2
 * @param resilienceEnabled 是否启用降级阶梯
 * @param localFloorEnabled 是否允许回落地板
 * @param sourceHolder      来源与降级计数持有者
 * @author jy
 * @since 1.0.0
 * @see LayeredCacheRegistry
 */
public record LayeredCacheDescriptor(String name,
                                     boolean l1Enabled,
                                     boolean l2Enabled,
                                     boolean resilienceEnabled,
                                     boolean localFloorEnabled,
                                     CacheSourceHolder sourceHolder) {
}
