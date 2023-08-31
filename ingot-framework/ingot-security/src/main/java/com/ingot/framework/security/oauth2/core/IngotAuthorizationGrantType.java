package com.ingot.framework.security.oauth2.core;

import com.ingot.framework.core.constants.SecurityConstants;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

/**
 * <p>Description  : IngotAuthorizationGrantType.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/5.</p>
 * <p>Time         : 4:01 下午.</p>
 */
public interface IngotAuthorizationGrantType {
    /**
     * 账号密码授权类型
     */
    AuthorizationGrantType PASSWORD = new AuthorizationGrantType(SecurityConstants.GrantType.PASSWORD);

    /**
     * 社交授权类型
     */
    AuthorizationGrantType SOCIAL = new AuthorizationGrantType(SecurityConstants.GrantType.SOCIAL);

    /**
     * 预授权类型
     */
    AuthorizationGrantType PRE_AUTHORIZATION_CODE = new AuthorizationGrantType(SecurityConstants.GrantType.PRE_AUTHORIZATION_CODE);
}
