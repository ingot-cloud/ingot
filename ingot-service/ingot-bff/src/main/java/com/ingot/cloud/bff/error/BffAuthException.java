package com.ingot.cloud.bff.error;

import com.ingot.framework.commons.model.bff.BffErrorCode;
import lombok.Getter;

/**
 * BFF 登录编排业务失败。
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
public class BffAuthException extends RuntimeException {
    private final BffErrorCode errorCode;

    public BffAuthException(BffErrorCode errorCode) {
        super(errorCode.getText());
        this.errorCode = errorCode;
    }
}
