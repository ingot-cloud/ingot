package com.ingot.framework.security.exception.oauth2;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.ingot.framework.security.provider.IngotOAuth2ExceptionSerializer;
import org.springframework.http.HttpStatus;

/**
 * <p>Description  : BadTenantException.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/26.</p>
 * <p>Time         : 2:43 下午.</p>
 */
@JsonSerialize(using = IngotOAuth2ExceptionSerializer.class)
public class BadTenantException extends IngotOAuth2Exception {

    public BadTenantException() {
        super("Bad tenant");
    }

    @Override
    public String getOAuth2ErrorCode() {
        return "Unauthorized";
    }

    @Override
    public int getHttpErrorCode() {
        return HttpStatus.UNAUTHORIZED.value();
    }
}
