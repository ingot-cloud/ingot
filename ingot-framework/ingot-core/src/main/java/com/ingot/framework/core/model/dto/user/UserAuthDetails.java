package com.ingot.framework.core.model.dto.user;

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
     * 租户ID
     */
    private Long tenantId;
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
     * 拥有的角色
     */
    private List<String> roles;
}