package com.ingot.framework.cache.config;

import java.time.Duration;

import lombok.Builder;
import lombok.Getter;

/**
 * <p>分层缓存各层的调参载体，由消费模块从自己的配置类映射而来。</p>
 *
 * <p>刻意不是 {@code @ConfigurationProperties}：各模块历史上已有各自的配置键命名
 * （凭证用 {@code l1-*}/{@code l2-*}，字典用 {@code cache-*}/{@code redis-*}），框架若强加统一前缀
 * 就会破坏这些已上线配置的兼容性。因此配置键的归属权留在消费模块，框架只接收映射后的结果。</p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * LayeredCacheSettings settings = LayeredCacheSettings.builder()
 *         .l1Enabled(properties.isCacheEnabled())
 *         .l1Ttl(properties.getCacheTtl())
 *         .l2Enabled(properties.isRedisEnabled())
 *         .l2Ttl(properties.getRedisTtl())
 *         .build();
 * }</pre>
 *
 * @author jy
 * @since 1.0.0
 * @see LayeredCacheBuilder
 */
@Getter
@Builder
public class LayeredCacheSettings {

    /**
     * 是否启用 L1 进程内缓存。
     */
    @Builder.Default
    private boolean l1Enabled = true;

    /**
     * L1 存活时间，同时充当失效广播丢失时的兜底收敛周期。
     */
    @Builder.Default
    private Duration l1Ttl = Duration.ofMinutes(5);

    /**
     * L1 最大条目数；单 key 场景取小值即可。
     */
    @Builder.Default
    private long l1MaximumSize = 16;

    /**
     * 是否启用 L2 Redis 共享缓存。
     */
    @Builder.Default
    private boolean l2Enabled = true;

    /**
     * L2 存活时间。
     */
    @Builder.Default
    private Duration l2Ttl = Duration.ofMinutes(30);

    /**
     * 是否启用 {@code remote → LKG → 地板} 降级阶梯；关闭后远端不可用直接上抛。
     */
    @Builder.Default
    private boolean resilienceEnabled = true;

    /**
     * 远端不可用且无 LKG 时是否回落地板；关闭后该场景 fail-closed 抛异常而非返回空。
     */
    @Builder.Default
    private boolean localFloorEnabled = true;

    /**
     * LKG 存活时间；{@code null} 表示长存不过期（推荐，故障可能持续任意久）。
     */
    private Duration lkgTtl;

    /**
     * 全默认配置。
     *
     * @return 默认设置实例
     */
    public static LayeredCacheSettings defaults() {
        return LayeredCacheSettings.builder().build();
    }
}
