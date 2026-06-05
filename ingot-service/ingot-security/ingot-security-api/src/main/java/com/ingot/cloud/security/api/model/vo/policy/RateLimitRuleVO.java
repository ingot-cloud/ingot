package com.ingot.cloud.security.api.model.vo.policy;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 限流规则 VO。
 *
 * @author jy
 * @since 2026/5/26
 */
@Data
public class RateLimitRuleVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String code;

    private String groupCode;

    private List<EndpointPatternVO> patternList;

    /**
     * 限流维度。同时兼容 DB 短码（IP/DV/UI）与枚举全名（IP/DEVICE/USER）。
     * SDK 内统一由 {@code RateLimitDimension.fromCode} 解析为枚举。
     */
    private String dimension;

    private int qps;

    private int burst;

    private int intervalSec;

    /**
     * F=快速失败 / Q=排队等待。
     */
    private String controlBehavior;

    private boolean enabled;

    private boolean dryRun;

    private int priority;

    private String remark;
}
