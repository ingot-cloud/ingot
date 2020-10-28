package com.ingot.framework.security.core.exception;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.ingot.framework.security.provider.IngotOAuth2ExceptionSerializer;
import org.springframework.http.HttpStatus;

/**
 * <p>Description  : ForbiddenException.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-08-22.</p>
 * <p>Time         : 14:38.</p>
 */
@JsonSerialize(using = IngotOAuth2ExceptionSerializer.class)
public class ForbiddenException extends IngotOAuth2Exception {

    public ForbiddenException(String msg, Throwable t) {
        super(msg);
    }

    @Override
    public String getOAuth2ErrorCode() {
        return "access_denied";
    }

    @Override
    public int getHttpErrorCode() {
        return HttpStatus.FORBIDDEN.value();
    }

}

