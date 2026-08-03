package com.ingot.framework.cache.source;

/**
 * <p>缓存值的生效来源，标识当前数据取自正常远端还是处于降级态。</p>
 *
 * <p>只表达降级阶梯上的三个位置，不包含 L1/L2 —— 热缓存命中时沿用被缓存那一刻的来源，
 * 不重新标记，否则可观测数据会被高频读取覆盖成无意义的「命中缓存」。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see CacheSourceHolder
 */
public enum CacheSource {

    /**
     * 远端数据源正常返回（含合法空）。
     */
    REMOTE,

    /**
     * 远端不可用，降级使用 Redis 中的最近成功快照。
     */
    LAST_KNOWN_GOOD,

    /**
     * 远端不可用且无最近成功快照，降级使用本地地板配置。
     */
    LOCAL_FLOOR
}
