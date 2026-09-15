package com.ingot.cloud.iam.evaluation;

import java.time.Duration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>映射 IAM 授权热缓存配置，关闭过期 LKG 放行，TTL 最长 30 秒。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ingot.iam.authorization-cache")
public class IamAuthorizationCacheProperties {
    /**
     * 是否启用 L1。默认 true。
     */
    private boolean l1Enabled = true;
    /**
     * L1 TTL，默认 30 秒；超过 30 秒时按 30 秒截断。
     */
    private Duration l1Ttl = Duration.ofSeconds(30);
    /**
     * L1 最大条目数。默认 10_000。
     */
    private long l1MaximumSize = 10_000;
    /**
     * 是否启用 L2 Redis。默认 true。
     */
    private boolean l2Enabled = true;
    /**
     * L2 TTL，默认 30 秒；超过 30 秒时按 30 秒截断。
     */
    private Duration l2Ttl = Duration.ofSeconds(30);
    /**
     * L2 key 前缀。默认 {@code in:iam:authorization:}。
     */
    private String redisKeyPrefix = "in:iam:authorization:";

    /**
     * 返回不超过 30 秒的 L1 TTL。
     *
     * @return 截断后的 TTL
     */
    public Duration boundedL1Ttl() {
        return bound(l1Ttl);
    }

    /**
     * 返回不超过 30 秒的 L2 TTL。
     *
     * @return 截断后的 TTL
     */
    public Duration boundedL2Ttl() {
        return bound(l2Ttl);
    }

    private static Duration bound(Duration ttl) {
        Duration max = Duration.ofSeconds(30);
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            return max;
        }
        return ttl.compareTo(max) > 0 ? max : ttl;
    }
}
