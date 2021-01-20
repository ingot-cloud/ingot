package com.ingot.framework.security.exception;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.ingot.framework.base.status.BaseStatusCode;
import com.ingot.framework.base.status.StatusCode;
import com.ingot.framework.security.provider.IngotOAuth2ExceptionSerializer;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;

/**
 * <p>Description  : IngotOAuth2Exception.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-08-22.</p>
 * <p>Time         : 14:27.</p>
 */
@JsonSerialize(using = IngotOAuth2ExceptionSerializer.class)
public class IngotOAuth2Exception extends OAuth2Exception {
    @Getter
    private String raw;
    private String oauth2ErrorCode = BaseStatusCode.INTERNAL_SERVER_ERROR.code();
    private int httpErrorCode = HttpStatus.BAD_REQUEST.value();

    public IngotOAuth2Exception(String code, String msg) {
        super(msg);
        this.oauth2ErrorCode = code;
    }

    public IngotOAuth2Exception(String code, String msg, Throwable cause) {
        super(msg, cause);
        this.oauth2ErrorCode = code;
    }

    public IngotOAuth2Exception(StatusCode statusCode) {
        this(statusCode.code(), statusCode.message());
    }

    public IngotOAuth2Exception(StatusCode statusCode, Throwable cause) {
        this(statusCode.code(), statusCode.message(), cause);
    }

    public IngotOAuth2Exception(StatusCode statusCode, OAuth2Exception e) {
        this(statusCode.code(), statusCode.message(), e);
        String oauth2ErrorCode = e.getOAuth2ErrorCode();
        Throwable cause = e.getCause();
        String msg = cause != null ? cause.getLocalizedMessage() : e.getLocalizedMessage();
        this.raw = String.format("errorCode: %s, message: %s", oauth2ErrorCode, msg);
    }

    public IngotOAuth2Exception(OAuth2Exception e) {
        super(e.getMessage(), e);
        String oauth2ErrorCode = e.getOAuth2ErrorCode();
        Throwable cause = e.getCause();
        this.httpErrorCode = e.getHttpErrorCode();
        String msg = cause != null ? cause.getLocalizedMessage() : e.getLocalizedMessage();
        this.raw = String.format("errorCode: %s, message: %s", oauth2ErrorCode, msg);
    }

    @Override
    public String getOAuth2ErrorCode() {
        return oauth2ErrorCode;
    }

    @Override
    public int getHttpErrorCode() {
        return httpErrorCode;
    }
}
