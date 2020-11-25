package com.ingot.framework.security.exception;

import com.ingot.framework.base.exception.BaseException;
import com.ingot.framework.security.status.SecurityStatusCode;

/**
 * <p>Description  : UserTokenSignBackException.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/2/20.</p>
 * <p>Time         : 5:35 PM.</p>
 */
public class UserTokenSignBackException extends BaseException {

    public UserTokenSignBackException() {
        super(SecurityStatusCode.TOKEN_SIGN_BACK);
    }

    public UserTokenSignBackException(String message) {
        super(SecurityStatusCode.TOKEN_SIGN_BACK.code(), message);
    }
}
