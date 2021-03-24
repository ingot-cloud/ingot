package com.ingot.cloud.pms.api.model.dto.user;

import com.ingot.cloud.pms.api.model.domain.SysUser;
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
public class UserDto extends SysUser {
    /**
     * 拥有的角色ID
     */
    private List<Long> roleIds;
    /**
     * 用于查询条件，可以登录的客户端ID
     */
    private List<Long> clientIds;
    /**
     * 新密码
     */
    private String newPassword;
}
