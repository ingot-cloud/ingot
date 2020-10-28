package com.ingot.framework.security.core.exception;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.ingot.framework.security.provider.IngotOAuth2ExceptionSerializer;
import org.springframework.http.HttpStatus;

/**
 * <p>Description  : MethodNotAllowedException.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-08-22.</p>
 * <p>Time         : 14:38.</p>
 */
@JsonSerialize(using = IngotOAuth2ExceptionSerializer.class)
public class MethodNotAllowedException extends IngotOAuth2Exception {

    public MethodNotAllowedException(String msg, Throwable t) {
        super(msg);
    }

    @Override
    public String getOAuth2ErrorCode() {
        return "method_not_allowed";
    }

    @Override
    public int getHttpErrorCode() {
        return HttpStatus.METHOD_NOT_ALLOWED.value();
    }

}
