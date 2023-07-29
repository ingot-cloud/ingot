package com.ingot.framework.security.oauth2.server.authorization.code;

/**
 * <p>Description  : PreAuthorizationCodeService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/28.</p>
 * <p>Time         : 2:56 PM.</p>
 */
public interface PreAuthorizationCodeService {

    /**
     * 保存授权信息
     *
     * @param authorization 授权信息
     * @param code          预授权code
     */
    void save(PreAuthorization authorization, String code);

    /**
     * 获取用户授权信息
     *
     * @param code 授权码
     */
    PreAuthorization get(String code);
}
