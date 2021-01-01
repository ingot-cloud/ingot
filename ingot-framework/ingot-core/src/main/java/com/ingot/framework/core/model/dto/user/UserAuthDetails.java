package com.ingot.framework.core.model.dto.user;

import com.ingot.framework.core.model.enums.UserStatusEnum;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * <p>Description  : UserAuthDetails.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/5.</p>
 * <p>Time         : 3:25 下午.</p>
 */
@Data
public class UserAuthDetails implements Serializable {
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
     * 授权类型
     */
    private String authType;
    /**
     * 状态, 0:正常，9:禁用
     */
    private UserStatusEnum status;
    /**
     * 拥有的角色编码
     */
    private List<String> roles;
}
