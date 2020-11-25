package com.ingot.framework.security.exception;


import com.ingot.framework.base.exception.BaseException;
import com.ingot.framework.security.status.SecurityStatusCode;

/**
 * <p>Description  : UserTokenInvalidException.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/6/1.</p>
 * <p>Time         : 下午4:23.</p>
 */
public class UserTokenInvalidException extends BaseException {

    public UserTokenInvalidException() {
        super(SecurityStatusCode.TOKEN_INVALID);
    }

    public UserTokenInvalidException(String message) {
        super(SecurityStatusCode.TOKEN_INVALID.code(), message);
    }
}
