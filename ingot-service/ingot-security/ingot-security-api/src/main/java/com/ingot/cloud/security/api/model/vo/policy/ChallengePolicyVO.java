package com.ingot.cloud.security.api.model.vo.policy;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * <p>挑战策略快照与管理面视图对象，对应表 {@code security_challenge_policy}。</p>
 *
 * <p>L6 执行面只认 {@link com.ingot.cloud.security.api.model.enums.ChallengeCaptchaType#IMAGE}
 * / {@link com.ingot.cloud.security.api.model.enums.ChallengeCaptchaType#SLIDER}；
 * {@code SMS}/{@code EMAIL} 可出现在历史行中，编译时跳过。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Data
public class ChallengePolicyVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 触发条件字面量 {@code always}（匹配即 412）。
     */
    public static final String TRIGGER_ALWAYS = "always";

    /**
     * 触发条件字面量 {@code on_rate_limit}（限流命中后 412）。
     */
    public static final String TRIGGER_ON_RATE_LIMIT = "on_rate_limit";

    /**
     * PassToken {@code scope} 最大长度，与表列 {@code varchar(64)} 一致。
     */
    public static final int SCOPE_MAX_LENGTH = 64;

    /**
     * {@code passTokenTtlSec} / {@code passTokenRemaining} 下限。
     */
    public static final int PASS_TOKEN_MIN = 1;

    private Long id;

    private String code;

    private String groupCode;

    private List<EndpointPatternVO> patternList;

    /**
     * 触发条件：{@link #TRIGGER_ALWAYS} 或 {@link #TRIGGER_ON_RATE_LIMIT}。
     * 登录失败锁定由 account-domain 处理，不支持 {@code on_failure_threshold}。
     */
    private String trigger;

    /**
     * 挑战类型：L6 仅 {@code SLIDER} / {@code IMAGE}（均走 {@code /vc/image}）。
     * 历史 {@code SMS}/{@code EMAIL} 行执行面跳过。
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
