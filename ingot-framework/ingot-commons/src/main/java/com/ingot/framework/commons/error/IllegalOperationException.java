package com.ingot.framework.commons.error;

import com.ingot.framework.commons.model.status.BaseErrorCode;

/**
 * <p>Description  : IllegalOperationException.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/10/10.</p>
 * <p>Time         : 4:15 下午.</p>
 */
public class IllegalOperationException extends BizException {

    public IllegalOperationException(String message) {
        super(BaseErrorCode.ILLEGAL_OPERATION, message);
    }

}
