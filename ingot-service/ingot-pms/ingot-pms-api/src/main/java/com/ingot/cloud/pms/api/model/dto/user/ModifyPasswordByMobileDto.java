package com.ingot.cloud.pms.api.model.dto.user;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;

/**
 * <p>Description  : ModifyPasswordByMobileDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/6/27.</p>
 * <p>Time         : 2:47 PM.</p>
 */
@Data
public class ModifyPasswordByMobileDto implements Serializable {

    @NotEmpty(message = "租户编码不能为空")
    private String tenantCode;

    @NotEmpty(message = "手机号不能为空")
    private String mobile;

    @NotEmpty(message = "新密码不能为空")
    private String new_password;

    @NotEmpty(message = "确认密码不能为空")
    private String confirm_password;
}
