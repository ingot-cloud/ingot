package com.ingot.framework.security.oauth2.server.authorization.authentication;

/**
 * <p>Description  : UserDetailsTokenProcessor.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/8/31.</p>
 * <p>Time         : 9:28 AM.</p>
 */
public interface UserDetailsTokenProcessor {

    /**
     * 加工 {@link OAuth2UserDetailsAuthenticationToken}
     *
     * @param in {@link OAuth2UserDetailsAuthenticationToken}
     * @return {@link OAuth2UserDetailsAuthenticationToken}
     */
    OAuth2UserDetailsAuthenticationToken process(OAuth2UserDetailsAuthenticationToken in);
}
