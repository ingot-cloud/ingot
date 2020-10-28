package com.ingot.cloud.pms.api.model.dto.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;

/**
 * <p>Description  : ModifyPasswordDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/10/24.</p>
 * <p>Time         : 下午2:18.</p>
 */
@Data
public class ModifyPasswordDto implements Serializable {

    @JsonIgnore
    private long tenantId;

    @JsonIgnore
    private String username;

    @NotEmpty(message = "原始密码不能为空")
    private String old_password;

    @NotEmpty(message = "新密码不能为空")
    private String new_password;

    @NotEmpty(message = "确认密码不能为空")
    private String confirm_password;

}
