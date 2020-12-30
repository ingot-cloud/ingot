package com.ingot.framework.security.exception.oauth2;

import com.ingot.framework.base.status.BaseStatusCode;
import org.springframework.http.HttpStatus;

/**
 * <p>Description  : ForbiddenException.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-08-22.</p>
 * <p>Time         : 14:38.</p>
 */
public class ForbiddenException extends IngotOAuth2Exception {

    public ForbiddenException(String msg) {
        super(BaseStatusCode.FORBIDDEN.code(), msg);
    }

    public ForbiddenException(String msg, Throwable t) {
        super(BaseStatusCode.FORBIDDEN.code(), msg, t);
    }

    @Override
    public int getHttpErrorCode() {
        return HttpStatus.FORBIDDEN.value();
    }

}

