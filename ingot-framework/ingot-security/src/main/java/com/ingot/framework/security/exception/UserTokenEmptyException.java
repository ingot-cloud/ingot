package com.ingot.framework.security.exception;

import com.ingot.framework.security.exception.oauth2.IngotOAuth2Exception;
import com.ingot.framework.security.status.SecurityStatusCode;
import org.springframework.http.HttpStatus;

/**
 * <p>Description  : UserTokenEmptyException.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/6/6.</p>
 * <p>Time         : 下午4:45.</p>
 */
public class UserTokenEmptyException extends IngotOAuth2Exception {
    public UserTokenEmptyException() {
        super(SecurityStatusCode.TOKEN_EMPTY);
    }

    @Override
    public int getHttpErrorCode() {
        return HttpStatus.BAD_REQUEST.value();
    }
}
