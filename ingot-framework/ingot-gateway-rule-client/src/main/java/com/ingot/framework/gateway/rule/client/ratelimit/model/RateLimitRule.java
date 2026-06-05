package com.ingot.framework.gateway.rule.client.ratelimit.model;

import com.ingot.framework.gateway.rule.client.model.EndpointPattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 限流规则定义。
 *
 * <p>{@link #groupCode} 与 {@link #patternList} 二选一：优先使用 groupCode 关联
 * {@link EndpointGroup}，为空则使用 patternList 内联模式。</p>
 *
 * <p>对应 {@code gateway_rate_limit_rule} 表。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RateLimitRule implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String code;

    private String groupCode;

    private List<EndpointPattern> patternList;

    private RateLimitDimension dimension;

    private int qps;

    private int burst;

    private int intervalSec;

    /**
     * 控制行为: F=快速失败, Q=排队等待。
     */
    private String controlBehavior;

    private boolean enabled;

    private boolean dryRun;

    private int priority;

    private String remark;
}
