package com.ingot.framework.security.exception;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.ingot.framework.security.provider.IngotOAuth2ExceptionSerializer;

/**
 * <p>Description  : InvalidException.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-08-22.</p>
 * <p>Time         : 14:38.</p>
 */
@JsonSerialize(using = IngotOAuth2ExceptionSerializer.class)
public class InvalidException extends IngotOAuth2Exception {

    public InvalidException(String msg, Throwable t) {
        super(msg, t);
    }

    @Override
    public String getOAuth2ErrorCode() {
        return "invalid_exception";
    }

    @Override
    public int getHttpErrorCode() {
        return 426;
    }

}
