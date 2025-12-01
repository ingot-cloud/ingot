package com.ingot.framework.commons.model.security;

import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * <p>Description  : ResetPwdVO.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/10/21.</p>
 * <p>Time         : 10:44 AM.</p>
 */
@Data
@Schema(description = "重置密码")
public class ResetPwdVO implements Serializable {
    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    private Long id;
    /**
     * 初始化随机验证码
     */
    @Schema(description = "初始化随机验证码")
    private String random;
}
