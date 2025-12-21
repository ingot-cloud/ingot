package com.ingot.cloud.auth.model.vo;

import com.ingot.framework.security.oauth2.server.authorization.OnlineToken;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>Description  : UserTokenVO.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/21.</p>
 * <p>Time         : 12:09.</p>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserTokenVO extends OnlineToken {
    /**
     * 昵称
     */
    private String nickname;
    /**
     * 头像
     */
    private String avatar;
    /**
     * 客户端名称
     */
    private String clientName;
    /**
     * 租户名称
     */
    private String tenantName;
    /**
     * 租户 Logo
     */
    private String tenantLogo;
}
