package com.ingot.framework.security.oauth2.server.authorization.code;

import com.ingot.framework.security.core.userdetails.IngotUser;

/**
 * <p>Description  : PreAuthorizationCodeService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/28.</p>
 * <p>Time         : 2:56 PM.</p>
 */
public interface PreAuthorizationCodeService {

    /**
     * 保存用户信息
     *
     * @param user 用户信息
     * @param code 预授权code
     */
    void saveUserInfo(IngotUser user, String code);

    /**
     * 获取用户信息
     *
     * @param code 授权码
     */
    IngotUser getUserInfo(String code);
}
