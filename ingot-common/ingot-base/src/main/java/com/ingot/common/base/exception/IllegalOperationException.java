package com.ingot.common.base.exception;

import static com.ingot.common.base.http.BaseStatusCode.ILLEGAL_OPERATION;

/**
 * <p>Description  : IllegalOperationException.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/10/10.</p>
 * <p>Time         : 4:15 下午.</p>
 */
public class IllegalOperationException extends BaseException {

    public IllegalOperationException(String message) {
        super(ILLEGAL_OPERATION, message);
    }

}
