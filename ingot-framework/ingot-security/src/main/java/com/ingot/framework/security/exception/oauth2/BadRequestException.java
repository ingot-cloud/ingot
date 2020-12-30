package com.ingot.framework.security.exception.oauth2;

import com.ingot.framework.base.status.BaseStatusCode;
import org.springframework.http.HttpStatus;

/**
 * <p>Description  : BadRequestException.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/12/30.</p>
 * <p>Time         : 3:15 下午.</p>
 */
public class BadRequestException extends IngotOAuth2Exception{

    public BadRequestException(String msg) {
        super(BaseStatusCode.BAD_REQUEST.code(), msg);
    }

    public BadRequestException(String msg, Throwable t) {
        super(BaseStatusCode.BAD_REQUEST.code(), msg, t);
    }

    @Override
    public int getHttpErrorCode() {
        return HttpStatus.BAD_REQUEST.value();
    }
}
