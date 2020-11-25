package com.ingot.framework.security.exception;

import com.ingot.framework.base.exception.BaseException;
import com.ingot.framework.security.status.SecurityStatusCode;

/**
 * <p>Description  : UserTokenEmptyException.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/6/6.</p>
 * <p>Time         : 下午4:45.</p>
 */
public class UserTokenEmptyException extends BaseException {
    public UserTokenEmptyException() {
        super(SecurityStatusCode.TOKEN_EMPTY);
    }
}
