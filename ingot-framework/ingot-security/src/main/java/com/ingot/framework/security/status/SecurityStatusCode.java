package com.ingot.framework.security.status;

import com.ingot.framework.common.status.StatusCode;

/**
 * <p>Description  : SecurityStatusCode.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/10/27.</p>
 * <p>Time         : 4:59 下午.</p>
 */
public enum SecurityStatusCode implements StatusCode {
    TOKEN_INVALID("S0001", "Token 失效"),
    TOKEN_EMPTY("S0002", "Token 为空"),
    TOKEN_SIGN_BACK("S0003", "Token 已被签退"),
    REFRESH_TOKEN_INVALID("S0004", "RefreshToken 失效");

    private final String code;
    private final String message;

    SecurityStatusCode(String code, String message){
        this.code = code;
        this.message = message;
    }

    @Override public String code() {
        return code;
    }

    @Override public String message() {
        return message;
    }
}
