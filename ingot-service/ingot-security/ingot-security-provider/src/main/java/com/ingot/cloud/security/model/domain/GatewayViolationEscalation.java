package com.ingot.cloud.security.model.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 限流违规升级全局配置实体，对应 {@code gateway_violation_escalation} 单行表。
 *
 * @author jy
 * @since 2026/6/5
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("gateway_violation_escalation")
public class GatewayViolationEscalation implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final long SINGLETON_ID = 1L;

    @TableId(type = IdType.INPUT)
    private Long id;

    /** 违规计数滑动窗口（秒）。 */
    private Integer windowSec;

    /** 窗口内限流拒绝次数阈值。 */
    private Integer blockThreshold;

    /** 临时封禁 TTL（秒）。 */
    private Integer tempBlockTtlSec;

    /** 是否启用违规升级。 */
    private Boolean enabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
