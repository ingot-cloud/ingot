package com.ingot.framework.security.exception;


import com.ingot.framework.security.status.SecurityStatusCode;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;

/**
 * <p>Description  : TokenInvalidException.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/6/1.</p>
 * <p>Time         : 下午4:23.</p>
 */
public class TokenInvalidException extends IngotOAuth2Exception {

    public TokenInvalidException() {
        super(SecurityStatusCode.TOKEN_INVALID);
    }

    public TokenInvalidException(String message) {
        super(SecurityStatusCode.TOKEN_INVALID.code(), message);
    }

    public TokenInvalidException(OAuth2Exception e){
        super(SecurityStatusCode.TOKEN_INVALID, e);
    }

    @Override
    public int getHttpErrorCode() {
        return HttpStatus.UNAUTHORIZED.value();
    }
}
