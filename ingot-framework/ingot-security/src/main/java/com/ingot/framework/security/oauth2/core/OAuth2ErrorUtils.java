package com.ingot.framework.security.oauth2.core;

import com.ingot.framework.core.model.support.R;
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
    public static final String CLIENT_ERROR_URI = "https://datatracker.ietf.org/doc/html/rfc6749#section-3.2.1";


    /**
     * 检测 Response，如果失败则 throw 认证异常
     *
     * @param response {@link R}
     */
    public static void checkResponse(R<?> r) {
        if (!r.isSuccess()) {
            throwAuthenticationException(r.getCode(), r.getMessage());
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
        throwAuthenticationException(code, desc, null);
    }

    /**
     * OAuth2 认证异常
     *
     * @param code     error code
     * @param desc     error description
     * @param errorUri uri
     */
    public static void throwAuthenticationException(String code, String desc, String errorUri) {
        throw new OAuth2AuthenticationException(new OAuth2Error(code, desc, errorUri));
    }

    /**
     * OAuth2 认证异常
     *
     * @param code  error code
     * @param desc  error description
     * @param cause the root cause
     */
    public static void throwAuthenticationException(String code, String desc, String errorUri, Throwable cause) {
        throw new OAuth2AuthenticationException(new OAuth2Error(code, desc, errorUri), cause);
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

    public static void throwInvalidRequestParameter(String parameterName, String errorUri) {
        throwAuthenticationException(
                OAuth2ErrorCodes.INVALID_REQUEST, "OAuth 2.0 Parameter: " + parameterName, errorUri);
    }

    public static void throwInvalidRequestParameter(String parameterName) {
        throwInvalidRequestParameter(parameterName, null);
    }

    public static void throwInvalidClient(String desc) {
        throwAuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT, desc);
    }

    public static void throwInvalidClientParameter(String parameterName) {
        throwAuthenticationException(
                OAuth2ErrorCodes.INVALID_CLIENT, "Client authentication failed: " + parameterName, CLIENT_ERROR_URI);
    }

    /**
     * 不允许访问客户端
     */
    public static void throwNotAllowClient(String desc) {
        throwAuthenticationException(OAuth2ErrorCodesExtension.NOT_ALLOW_CLIENT.getCode(), desc);
    }

    public static void throwPreAuthorizationCodeExpired(String desc) {
        throwAuthenticationException(OAuth2ErrorCodesExtension.PRE_AUTHORIZATION_CODE_EXPIRED.getCode(), desc);
    }
}
