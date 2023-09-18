package com.ingot.cloud.pms.api.model.dto.user;

import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.framework.core.utils.validation.Group;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>Description  : UserQueryDTO.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/18.</p>
 * <p>Time         : 11:44 AM.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserQueryDTO extends SysUser {
    /**
     * 角色ID
     */
    private Long roleId;
    /**
     * 部门ID
     */
    private Long deptId;
    /**
     * 密码
     */
    @NotBlank(message = "{SysUser.password}", groups = Group.Create.class)
    private String newPassword;
}
