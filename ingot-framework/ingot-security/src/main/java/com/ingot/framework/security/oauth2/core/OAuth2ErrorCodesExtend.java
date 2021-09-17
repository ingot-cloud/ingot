package com.ingot.framework.security.oauth2.core;

import com.ingot.framework.common.status.StatusCode;

/**
 * <p>Description  : OAuth2ErrorCodesExtend.
 * {@link org.springframework.security.oauth2.core.OAuth2ErrorCodes}</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/17.</p>
 * <p>Time         : 2:33 下午.</p>
 */
public enum OAuth2ErrorCodesExtend implements StatusCode {
    USER_STATUS("user_status", "用户状态异常");

    private final String code;
    private final String message;

    OAuth2ErrorCodesExtend(String code, String message) {
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
