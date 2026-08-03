package com.ingot.cloud.security.model.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ingot.cloud.security.api.model.enums.LoginFailureDimension;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 登录失败保护策略实体。
 *
 * @author jy
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("login_failure_protection_policy")
public class LoginFailureProtectionPolicy implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private LoginFailureDimension dimension;

    private Boolean enabled;

    private Integer maxAttempts;

    private Integer windowMinutes;

    private Integer blockTtlSec;

    private String blockKeyType;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
