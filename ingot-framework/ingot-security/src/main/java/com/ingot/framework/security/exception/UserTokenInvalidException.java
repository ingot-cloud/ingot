package com.ingot.framework.security.exception;


import com.ingot.framework.security.exception.oauth2.IngotOAuth2Exception;
import com.ingot.framework.security.status.SecurityStatusCode;
import org.springframework.http.HttpStatus;

/**
 * <p>Description  : UserTokenInvalidException.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/6/1.</p>
 * <p>Time         : 下午4:23.</p>
 */
public class UserTokenInvalidException extends IngotOAuth2Exception {

    public UserTokenInvalidException() {
        super(SecurityStatusCode.TOKEN_INVALID);
    }

    public UserTokenInvalidException(String message) {
        super(SecurityStatusCode.TOKEN_INVALID.code(), message);
    }

    @Override
    public int getHttpErrorCode() {
        return HttpStatus.UNAUTHORIZED.value();
    }
}
