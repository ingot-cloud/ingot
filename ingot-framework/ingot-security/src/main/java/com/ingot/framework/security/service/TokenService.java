package com.ingot.framework.security.service;

import org.springframework.security.oauth2.common.OAuth2AccessToken;

import javax.servlet.http.HttpServletRequest;

/**
 * <p>Description  : TokenService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/28.</p>
 * <p>Time         : 4:57 下午.</p>
 */
public interface TokenService {

    /**
     * 获取请求中的 token
     *
     * @param request HttpServletRequest
     * @return OAuth2AccessToken
     */
    OAuth2AccessToken getToken(HttpServletRequest request);

    /**
     * 检验 Token 有效性
     *
     * @param token OAuth2AccessToken
     */
    void checkAuthentication(OAuth2AccessToken token);
}
