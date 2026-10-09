package com.ingot.framework.authorization;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>远程授权 SDK 的缓存配置，期限仍受 IAM 绝对截止约束。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ingot.iam.authorization")
public class AuthorizationSdkProperties {

    private static final long DEFAULT_MAXIMUM_SIZE = 1000;

    /**
     * 是否启用进程内热缓存。
     */
    private boolean l1Enabled = true;

    /**
     * 是否启用 Redis 热缓存。
     */
    private boolean l2Enabled = true;

    /**
     * 请求热窗口，最大三十秒。
     */
    private Duration ttl = AuthorizationSdkConfiguration.MAX_TTL;

    /**
     * 本节点最大热缓存条数。
     */
    private long maximumSize = DEFAULT_MAXIMUM_SIZE;

}
