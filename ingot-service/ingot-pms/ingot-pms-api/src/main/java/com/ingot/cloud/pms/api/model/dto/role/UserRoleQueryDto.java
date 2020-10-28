package com.ingot.cloud.pms.api.model.dto.role;

import com.ingot.framework.base.model.dto.BaseQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;

/**
 * <p>Description  : UserRoleQueryDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/11/5.</p>
 * <p>Time         : 1:14 PM.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserRoleQueryDto extends BaseQueryDto {

    @NotBlank(message = "用户id不能为空")
    private String user_id;
    private String role_code;
    private String role_name;
}
