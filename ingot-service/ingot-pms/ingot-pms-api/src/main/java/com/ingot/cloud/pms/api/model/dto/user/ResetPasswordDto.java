package com.ingot.cloud.pms.api.model.dto.user;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;

/**
 * <p>Description  : ResetPasswordDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/10/24.</p>
 * <p>Time         : 下午4:04.</p>
 */
@Data
public class ResetPasswordDto implements Serializable {

    @NotEmpty(message = "租户编码不能为空")
    private String tenantCode;

    @NotEmpty(message = "用户名不能为空")
    private String username;

    @NotEmpty(message = "新密码不能为空")
    private String new_password;

    @NotEmpty(message = "确认密码不能为空")
    private String confirm_password;
}
