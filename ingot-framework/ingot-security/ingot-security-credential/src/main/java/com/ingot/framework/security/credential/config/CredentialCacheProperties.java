package com.ingot.framework.security.credential.config;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

/**
 * 凭证策略多级缓存配置项。
 *
 * <pre>
 * ingot.security.credential.cache.l1-enabled        # 是否启用 L1 Caffeine
 * ingot.security.credential.cache.l1-ttl            # L1 TTL（默认 5 分钟）
 * ingot.security.credential.cache.l1-maximum-size   # L1 最大条目数（实际单 key，但保留容量）
 * ingot.security.credential.cache.l2-enabled        # 是否启用 L2 Redis
 * ingot.security.credential.cache.l2-ttl            # L2 TTL（默认 30 分钟）
 * ingot.security.credential.cache.l2-key-prefix     # L2 Redis key 前缀
 * ingot.security.credential.cache.invalidation-enabled  # 是否启用跨节点失效订阅
 * </pre>
 *
 * @author jy
 * @since 2026/5/16
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ingot.security.credential.cache")
public class CredentialCacheProperties {

    /**
     * 是否启用 L1 Caffeine 缓存。
     */
    private boolean l1Enabled = true;

    /**
     * L1 Caffeine TTL。无单位数值按分钟解析（如 {@code 5} 表示 5 分钟），避免误当成毫秒导致写入即过期。
     */
    @DurationUnit(ChronoUnit.MINUTES)
    private Duration l1Ttl = Duration.ofMinutes(5);

    /**
     * L1 Caffeine 最大条目数（当前实现为单 key，但仍可配置以备扩展）。
     */
    private long l1MaximumSize = 16;

    /**
     * 是否启用 L2 Redis 共享缓存。
     */
    private boolean l2Enabled = true;

    /**
     * L2 Redis TTL。无单位数值按分钟解析（如 {@code 30} 表示 30 分钟），避免误当成毫秒导致写入即过期。
     */
    @DurationUnit(ChronoUnit.MINUTES)
    private Duration l2Ttl = Duration.ofMinutes(30);

    /**
     * L2 Redis key 前缀（最终 key = {@code <l2KeyPrefix>all}）。
     */
    private String l2KeyPrefix = "in:credential:configs:";

    /**
     * 是否启用跨节点失效广播订阅（依赖 ingot-event-bus）。
     */
    private boolean invalidationEnabled = true;
}
