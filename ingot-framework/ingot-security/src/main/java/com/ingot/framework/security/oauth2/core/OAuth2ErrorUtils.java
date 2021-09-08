package com.ingot.framework.security.oauth2.core;

import com.ingot.framework.core.wrapper.IngotResponse;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

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
     * @param response {@link IngotResponse}
     */
    public static void checkResponse(IngotResponse<?> response) {
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
        throwAuthenticationException(code, null, null);
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
     * @param code  error code
     * @param desc  error description
     * @param cause the root cause
     */
    public static void throwAuthenticationException(String code, String desc, Throwable cause) {
        throw new OAuth2AuthenticationException(new OAuth2Error(code, desc, null), cause);
    }
}
