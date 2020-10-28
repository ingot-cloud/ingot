package com.ingot.framework.security.core.exception;

import com.ingot.framework.base.exception.BaseException;
import com.ingot.framework.base.status.BaseStatusCode;

/**
 * <p>Description  : UserForbiddenException.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/9/30.</p>
 * <p>Time         : 8:30 下午.</p>
 */
public class UserForbiddenException extends BaseException {
    public UserForbiddenException() {
        super(BaseStatusCode.FORBIDDEN);
    }
}
