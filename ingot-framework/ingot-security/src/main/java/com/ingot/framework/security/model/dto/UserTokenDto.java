package com.ingot.framework.security.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>Description  : UserTokenDto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/14.</p>
 * <p>Time         : 下午2:23.</p>
 */
@Data
public class UserTokenDto implements Serializable {
    private static final long serialVersionUID = 313672371231575367L;

    /**
     * token 授权类型
     */
    private String authType;

    /**
     * 用户 Id
     */
    private String userId;

    /**
     * 用户登录名称
     */
    private String username;

    /**
     * 租户 Id
     */
    private String tenantId;

    /**
     * 用户 token
     */
    private String accessToken;

}
