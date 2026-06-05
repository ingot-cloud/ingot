package com.ingot.cloud.security.api.model.vo.policy;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 挑战策略 VO（Phase 4 启用）。
 *
 * @author jy
 * @since 2026/5/26
 */
@Data
public class ChallengePolicyVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String code;

    private String groupCode;

    private List<EndpointPatternVO> patternList;

    /**
     * always / on_rate_limit。登录失败锁定由 account-domain 处理，不支持 on_failure_threshold。
     */
    private String trigger;

    /**
     * 挑战类型: SLIDER / IMAGE / SMS / EMAIL。
     */
    private String challengeType;

    /**
     * 已废弃字段，保留列兼容；网关执行面不读取。
     */
    private String failureDimension;

    /**
     * 已废弃字段，保留列兼容；网关执行面不读取。
     */
    private Integer failureThreshold;

    /**
     * 已废弃字段，保留列兼容；网关执行面不读取。
     */
    private Integer failureWindowSec;

    /**
     * PassToken 有效期（秒）。
     */
    private Integer passTokenTtlSec;

    /**
     * PassToken 可消费次数。
     */
    private Integer passTokenRemaining;

    /**
     * 管理面保留字段；网关 Phase 1 未实现验码失败拉黑（临时封禁由限流违规计数触发）。
     */
    private Integer challengeFailureLimit;

    /**
     * 管理面保留字段；网关限流违规封禁时长见 {@code SentinelBlockHandler} 常量。
     */
    private Integer blockTtlSec;

    /**
     * 策略作用域（与 PassToken scope 关联）。
     */
    private String scope;

    private boolean enabled;

    private int priority;

    private String remark;
}
