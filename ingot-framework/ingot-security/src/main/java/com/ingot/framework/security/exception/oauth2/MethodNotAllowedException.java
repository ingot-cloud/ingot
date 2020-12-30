package com.ingot.framework.security.exception.oauth2;

import com.ingot.framework.base.status.BaseStatusCode;
import org.springframework.http.HttpStatus;

/**
 * <p>Description  : MethodNotAllowedException.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-08-22.</p>
 * <p>Time         : 14:38.</p>
 */
public class MethodNotAllowedException extends IngotOAuth2Exception {

    public MethodNotAllowedException(String msg) {
        super(BaseStatusCode.METHOD_NOT_ALLOWED.code(), msg);
    }

    public MethodNotAllowedException(String msg, Throwable t) {
        super(BaseStatusCode.METHOD_NOT_ALLOWED.code(), msg, t);
    }

    @Override
    public int getHttpErrorCode() {
        return HttpStatus.METHOD_NOT_ALLOWED.value();
    }

}
