package com.ingot.cloud.pms.api.model.dto.user;

import com.ingot.framework.base.model.dto.BaseQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;

/**
 * <p>Description  : SpecifiedRoleUserQueryDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/6/27.</p>
 * <p>Time         : 3:28 PM.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SpecifiedRoleUserQueryDto extends BaseQueryDto {
    @NotBlank(message = "角色id不能为空")
    private String role_id;
    private String username;
    private String real_name;
}