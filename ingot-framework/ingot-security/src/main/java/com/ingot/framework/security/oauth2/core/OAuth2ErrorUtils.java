package com.ingot.framework.security.oauth2.core;

import com.ingot.framework.core.wrapper.R;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;

/**
 * <p>Description  : OAuth2ErrorUtils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/7.</p>
 * <p>Time         : 4:44 下午.</p>
 */
public final class OAuth2ErrorUtils {

    /**
     * 检测 Response，如果失败则 throw 认证异常
     *
     * @param response {@link R}
     */
    public static void checkResponse(R<?> response) {
        if (!response.isSuccess()) {
            throwAuthenticationException(response.getCode(), response.getMessage());
        }
    }

    /**
     * OAuth2 认证异常
     *
     * @param code error code
     */
    public static void throwAuthenticationException(String code) {
        throwAuthenticationException(code, null);
    }

    /**
     * OAuth2 认证异常
     *
     * @param code error code
     */
    public static void throwAuthenticationException(OAuth2ErrorCodesExtension code) {
        throwAuthenticationException(code.getCode(), code.getText());
    }

    /**
     * OAuth2 认证异常
     *
     * @param code error code
     * @param desc error description
     */
    public static void throwAuthenticationException(String code, String desc) {
        throw new OAuth2AuthenticationException(new OAuth2Error(code, desc, null));
    }

    /**
     * OAuth2 认证异常
     *
     * @param code  error code
     * @param desc  error description
     * @param cause the root cause
     */
    public static void throwAuthenticationException(String code, String desc, Throwable cause) {
        throw new OAuth2AuthenticationException(new OAuth2Error(code, desc, null), cause);
    }

    /**
     * 用户已签退异常
     */
    public static void throwSignOut() {
        throwAuthenticationException(OAuth2ErrorCodesExtension.SIGN_OUT);
    }

    /**
     * 用户Token失效异常
     */
    public static void throwInvalidToken() {
        throwAuthenticationException(OAuth2ErrorCodes.INVALID_TOKEN);
    }

    /**
     * 无效请求
     */
    public static void throwInvalidRequest() {
        throwAuthenticationException(OAuth2ErrorCodes.INVALID_REQUEST);
    }

    public static void throwInvalidRequest(String desc) {
        throwAuthenticationException(OAuth2ErrorCodes.INVALID_REQUEST, desc);
    }
}
