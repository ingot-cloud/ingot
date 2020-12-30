package com.ingot.framework.security.exception.oauth2;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.ingot.framework.base.status.BaseStatusCode;
import com.ingot.framework.security.provider.IngotOAuth2ExceptionSerializer;
import org.springframework.http.HttpStatus;

/**
 * <p>Description  : BadTenantException.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/26.</p>
 * <p>Time         : 2:43 下午.</p>
 */
public class BadTenantException extends IngotOAuth2Exception {

    public BadTenantException(String msg) {
        super(BaseStatusCode.UNAUTHORIZED.code(), msg);
    }

    public BadTenantException() {
        super(BaseStatusCode.UNAUTHORIZED.code(), "Bad tenant");
    }

    @Override
    public int getHttpErrorCode() {
        return HttpStatus.UNAUTHORIZED.value();
    }
}
