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
     * @param code Error code
     * @param desc Error description
     */
    public static void throwAuthenticationException(String code, String desc) {
        throw new OAuth2AuthenticationException(new OAuth2Error(code, desc, null));
    }
}
