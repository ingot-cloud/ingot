package com.ingot.framework.security.oauth2.core;

import com.ingot.framework.common.status.StatusCode;

/**
 * <p>Description  : OAuth2ErrorCodesExtension.
 * {@link org.springframework.security.oauth2.core.OAuth2ErrorCodes}</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/17.</p>
 * <p>Time         : 2:33 下午.</p>
 */
public enum OAuth2ErrorCodesExtension implements StatusCode {
    USER_STATUS("user_status", "Illegal user status"),
    SIGN_OUT("user_sign_out", "User has been signed out");

    private final String code;
    private final String message;

    OAuth2ErrorCodesExtension(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
