package com.ingot.framework.security.core.userdetails;

import org.springframework.security.oauth2.core.AuthorizationGrantType;

/**
 * <p>Description  : UserDetailsModeEnum.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/5.</p>
 * <p>Time         : 4:01 下午.</p>
 */
public interface UserDetailsAuthorizationGrantType {
    /**
     * 账号密码授权类型
     */
    AuthorizationGrantType PASSWORD = AuthorizationGrantType.PASSWORD;

    /**
     * 社交授权类型
     */
    AuthorizationGrantType SOCIAL = new AuthorizationGrantType("social");
}
