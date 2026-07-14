package com.ingot.framework.security.replay;

import java.time.Duration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>防重放配置属性，前缀 {@code ingot.replay}。</p>
 *
 * <p>统一管理时间戳窗口、时钟偏移、nonce 存储 TTL、Key 前缀与存储不可用时的降级策略，
 * 供加密传输、验签及业务幂等等场景共享。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ingot.security.replay")
public class ReplayProperties {
    /**
     * 是否启用防重放校验
     */
    private boolean enabled = true;
    /**
     * nonce 去重的存活时长，同时作为 nonce 判重的有效窗口
     */
    private Duration window = Duration.ofMinutes(5);
    /**
     * 允许的时钟偏移，用于时间戳窗口校验
     */
    private Duration clockSkew = Duration.ofMinutes(5);
    /**
     * 存储 Key 前缀
     */
    private String keyPrefix = "replay:";
    /**
     * 防重放存储不可用时的策略：true=放行(fail-open)，false=拒绝(fail-close)
     */
    private boolean failOpen = false;
}
