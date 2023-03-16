package com.ingot.framework.core.error.exception;

import com.ingot.framework.core.model.status.BaseStatusCode;

/**
 * <p>Description  : IllegalOperationException.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/10/10.</p>
 * <p>Time         : 4:15 下午.</p>
 */
public class IllegalOperationException extends BizException {

    public IllegalOperationException(String message) {
        super(BaseStatusCode.ILLEGAL_OPERATION, message);
    }

}
