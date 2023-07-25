package com.ingot.framework.vc.common;

import com.ingot.framework.core.error.exception.BizException;

/**
 * <p>Description  : VCException.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/4/27.</p>
 * <p>Time         : 12:10 PM.</p>
 */
public class VCException extends BizException {
    public VCException() {
        super(VCErrorCode.Illegal);
    }

    public VCException(String message) {
        super(VCErrorCode.Illegal.getCode(), message);
    }

    public VCException(VCErrorCode code, String message) {
        super(code.getCode(), message);
    }
}
