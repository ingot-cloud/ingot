package com.ingot.framework.security.oauth2.server.authorization.code;

/**
 * <p>Description  : OAuth2PreAuthorizationService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/28.</p>
 * <p>Time         : 2:56 PM.</p>
 */
public interface OAuth2PreAuthorizationService {

    /**
     * 保存授权信息
     *
     * @param authorization 授权信息
     */
    void save(OAuth2PreAuthorization authorization);

    /**
     * 获取用户授权信息
     *
     * @param code 授权码
     */
    OAuth2PreAuthorization get(String code);
}
