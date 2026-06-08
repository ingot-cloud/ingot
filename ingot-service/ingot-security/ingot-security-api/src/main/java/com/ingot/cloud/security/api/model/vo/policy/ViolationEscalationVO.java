package com.ingot.cloud.security.api.model.vo.policy;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 限流违规升级全局配置 VO，对应 {@code gateway_violation_escalation} 单行表。
 *
 * @author jy
 * @since 2026/6/5
 */
@Data
public class ViolationEscalationVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键，固定为 1。 */
    private Long id;

    /** 违规计数滑动窗口（秒）。 */
    private int windowSec;

    /** 窗口内限流拒绝次数达到该值即临时封禁。 */
    private int blockThreshold;

    /** 临时封禁 TTL（秒）。 */
    private int tempBlockTtlSec;

    /** 是否启用违规升级逻辑。 */
    private boolean enabled;
}
