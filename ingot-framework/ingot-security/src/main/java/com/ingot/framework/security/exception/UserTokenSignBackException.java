package com.ingot.framework.security.exception;

import com.ingot.framework.security.exception.oauth2.IngotOAuth2Exception;
import com.ingot.framework.security.status.SecurityStatusCode;
import org.springframework.http.HttpStatus;

/**
 * <p>Description  : UserTokenSignBackException.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/2/20.</p>
 * <p>Time         : 5:35 PM.</p>
 */
public class UserTokenSignBackException extends IngotOAuth2Exception {

    public UserTokenSignBackException() {
        super(SecurityStatusCode.TOKEN_SIGN_BACK);
    }

    public UserTokenSignBackException(String message) {
        super(SecurityStatusCode.TOKEN_SIGN_BACK.code(), message);
    }

    @Override
    public int getHttpErrorCode() {
        return HttpStatus.UNAUTHORIZED.value();
    }
}
