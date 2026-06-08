package com.ingot.framework.gateway.rule.client.violation.model;

import lombok.Builder;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 限流违规升级运行时配置：滑动窗口、封禁阈值、临时封禁 TTL。
 *
 * <p>由 {@link com.ingot.framework.gateway.rule.client.violation.ViolationEscalationService} 提供；
 * local 模式来自 yaml，remote 模式来自 ingot-security 快照。</p>
 *
 * @author jy
 * @since 2026/6/5
 */
@Getter
@Builder
public class ViolationEscalationConfig implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final int DEFAULT_WINDOW_SEC = 60;
    public static final int DEFAULT_BLOCK_THRESHOLD = 30;
    public static final int DEFAULT_TEMP_BLOCK_TTL_SEC = 900;

    /** 违规计数滑动窗口（秒）。 */
    private final int windowSec;

    /** 窗口内限流拒绝次数达到该值即临时封禁。 */
    private final int blockThreshold;

    /** 临时封禁 Redis TTL（秒）。 */
    private final int tempBlockTtlSec;

    /** 为 false 时不累加违规计数、不写入临时封禁。 */
    private final boolean enabled;

    /** 配置版本号，用于缓存失效与启动日志。 */
    private final long version;

    public static ViolationEscalationConfig defaults() {
        return ViolationEscalationConfig.builder()
                .windowSec(DEFAULT_WINDOW_SEC)
                .blockThreshold(DEFAULT_BLOCK_THRESHOLD)
                .tempBlockTtlSec(DEFAULT_TEMP_BLOCK_TTL_SEC)
                .enabled(true)
                .version(0L)
                .build();
    }
}
