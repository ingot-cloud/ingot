package com.ingot.cloud.pms.api.model.dto.user;

import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.framework.core.utils.validation.Group;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * <p>Description  : UserDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/1/6.</p>
 * <p>Time         : 2:09 下午.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserDTO extends SysUser {
    /**
     * 拥有的角色ID
     */
    private List<Long> roleIds;
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
