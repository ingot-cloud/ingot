package com.ingot.framework.commons.model.security;

import java.io.Serializable;
import java.util.List;

import com.ingot.framework.commons.model.common.AllowTenantDTO;
import com.ingot.framework.commons.model.enums.UserStatusEnum;
import lombok.Data;

/**
 * <p>Description  : UserDetailsResponse.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/5.</p>
 * <p>Time         : 3:25 下午.</p>
 */
@Data
public class UserDetailsResponse implements Serializable {
    /**
     * 用户类型 {@link UserTypeEnum}
     */
    private String userType;
    /**
     * 用户ID
     */
    private Long id;
    /**
     * 默认登录tenant
     */
    private Long tenant;
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
     * 可以访问的租户列表
     */
    private List<AllowTenantDTO> allows;
}
