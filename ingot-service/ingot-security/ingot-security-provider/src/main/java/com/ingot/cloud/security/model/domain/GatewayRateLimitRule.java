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
 * 网关限流规则实体。
 *
 * @author jy
 * @since 2026/5/26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "gateway_rate_limit_rule", autoResultMap = true)
public class GatewayRateLimitRule implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String code;

    private String groupCode;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<EndpointPatternVO> patternList;

    /**
     * 限流维度短码（DB {@code char(2)}）：{@code IP} / {@code DV}(device) / {@code UI}(user)。
     */
    private String dimension;

    private Integer qps;

    private Integer burst;

    private Integer intervalSec;

    /**
     * F=快速失败 / Q=排队等待。
     */
    private String controlBehavior;

    private Boolean enabled;

    private Boolean dryRun;

    private Integer priority;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
