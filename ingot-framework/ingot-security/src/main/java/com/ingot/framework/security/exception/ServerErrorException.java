package com.ingot.framework.security.exception;

import com.ingot.framework.base.status.BaseStatusCode;
import org.springframework.http.HttpStatus;

/**
 * <p>Description  : ServerErrorException.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-08-22.</p>
 * <p>Time         : 14:38.</p>
 */
public class ServerErrorException extends IngotOAuth2Exception {

    public ServerErrorException(String msg) {
        super(BaseStatusCode.INTERNAL_SERVER_ERROR.code(), msg);
    }

    public ServerErrorException(String msg, Throwable t) {
        super(BaseStatusCode.INTERNAL_SERVER_ERROR.code(), msg, t);
    }

    @Override
    public int getHttpErrorCode() {
        return HttpStatus.INTERNAL_SERVER_ERROR.value();
    }

}