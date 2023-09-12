package com.ingot.framework.security.core.userdetails;

import com.ingot.framework.core.model.common.AllowTenantDTO;
import com.ingot.framework.core.model.enums.UserStatusEnum;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * <p>Description  : UserDetailsResponse.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/5.</p>
 * <p>Time         : 3:25 下午.</p>
 */
@Data
public class UserDetailsResponse implements Serializable {
    /**
     * 用户类型 {@link com.ingot.framework.security.common.constants.UserType}
     */
    private String userType;
    /**
     * 用户ID
     */
    private Long id;
    /**
     * 部门ID
     */
    private Long deptId;
    /**
     * 用户名
     */
    private String username;
    /**
     * 密码
     */
    private String password;
    /**
     * 状态, 0:正常，9:禁用
     */
    private UserStatusEnum status;
    /**
     * 权限列表，roleCode以及authorityCode
     */
    private List<String> roles;
    /**
     * 客户端列表，clientId
     */
    private List<String> clients;
    /**
     * 可以访问的租户列表
     */
    private List<AllowTenantDTO> allows;
}
