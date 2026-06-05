package com.ingot.cloud.security.model.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.ingot.cloud.security.api.model.vo.policy.EndpointPatternVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 挑战策略实体。
 *
 * @author jy
 * @since 2026/5/26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "security_challenge_policy", autoResultMap = true)
public class SecurityChallengePolicy implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String code;

    private String groupCode;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<EndpointPatternVO> patternList;

    /**
     * always / on_rate_limit。登录失败锁定由 account-domain 处理。
     */
    @TableField("`trigger`")
    private String trigger;

    /**
     * SLIDER / IMAGE / SMS / EMAIL。
     */
    private String challengeType;

    /**
     * IP / UI / DV。
     */
    private String failureDimension;

    private Integer failureThreshold;

    private Integer failureWindowSec;

    private Integer passTokenTtlSec;

    private Integer passTokenRemaining;

    private Integer challengeFailureLimit;

    private Integer blockTtlSec;

    private String scope;

    private Boolean enabled;

    private Integer priority;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
