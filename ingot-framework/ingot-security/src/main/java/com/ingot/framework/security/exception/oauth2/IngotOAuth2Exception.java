package com.ingot.framework.security.exception.oauth2;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.ingot.framework.base.status.BaseStatusCode;
import com.ingot.framework.base.status.StatusCode;
import com.ingot.framework.security.provider.IngotOAuth2ExceptionSerializer;
import lombok.Getter;
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

    public IngotOAuth2Exception(OAuth2Exception e) {
        super(e.getMessage(), e);
        String oauth2ErrorCode = e.getOAuth2ErrorCode();
        Throwable cause = e.getCause();
        this.raw = cause != null ?
                String.format("errorCode: %s, raw: %s", oauth2ErrorCode, cause.getMessage()) :
                oauth2ErrorCode;
    }

    @Override
    public String getOAuth2ErrorCode() {
        return oauth2ErrorCode;
    }
}
