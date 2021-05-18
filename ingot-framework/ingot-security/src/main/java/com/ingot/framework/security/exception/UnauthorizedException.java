package com.ingot.framework.security.exception;

import com.ingot.framework.common.status.BaseStatusCode;
import org.springframework.http.HttpStatus;

/**
 * <p>Description  : UnauthorizedException.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-08-22.</p>
 * <p>Time         : 14:38.</p>
 */
public class UnauthorizedException extends IngotOAuth2Exception {

    public UnauthorizedException(String msg) {
        super(BaseStatusCode.UNAUTHORIZED.code(), msg);
    }

    public UnauthorizedException(String msg, Throwable t) {
        super(BaseStatusCode.UNAUTHORIZED.code(), msg, t);
    }

    @Override
    public int getHttpErrorCode() {
        return HttpStatus.UNAUTHORIZED.value();
    }
}
