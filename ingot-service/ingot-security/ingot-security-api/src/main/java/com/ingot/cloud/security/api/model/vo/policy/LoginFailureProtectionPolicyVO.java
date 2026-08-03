package com.ingot.cloud.security.api.model.vo.policy;

import com.ingot.cloud.security.api.model.enums.LoginFailureDimension;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 登录失败保护策略视图对象。
 *
 * @author jy
 * @since 1.0.0
 */
@Data
public class LoginFailureProtectionPolicyVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private LoginFailureDimension dimension;
    private Boolean enabled;
    private Integer maxAttempts;
    private Integer windowMinutes;
    private Integer blockTtlSec;
    private String blockKeyType;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
