package com.ingot.framework.security.core.userdetails;

import java.io.Serializable;
import java.util.List;

import com.ingot.framework.core.model.enums.UserStatusEnum;
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
     * 客户端ID
     */
    private String clientId;
    /**
     * 用户ID
     */
    private Long id;
    /**
     * 部门ID
     */
    private Long deptId;
    /**
     * 租户ID
     */
    private Integer tenantId;
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
     * token认证方法
     */
    private String tokenAuthenticationMethod;
    /**
     * 拥有的角色编码
     */
    private List<String> roles;
}
