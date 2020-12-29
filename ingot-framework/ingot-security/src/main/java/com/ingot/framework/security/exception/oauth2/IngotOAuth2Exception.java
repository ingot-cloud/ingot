package com.ingot.framework.security.exception.oauth2;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.ingot.framework.base.status.BaseStatusCode;
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
    private final String code = BaseStatusCode.INTERNAL_SERVER_ERROR.code();
    @Getter
    private String errorCode;

    public IngotOAuth2Exception(String msg){
        super(msg);
    }

    public IngotOAuth2Exception(String msg, Throwable t) {
        super(msg, t);
    }

    public IngotOAuth2Exception(OAuth2Exception e) {
        super(e.getMessage(), e);
        String oauth2ErrorCode = e.getOAuth2ErrorCode();
        Throwable cause = e.getCause();
        this.errorCode = cause != null ?
                String.format("errorCode: %s, raw: %s", oauth2ErrorCode, cause.getMessage()) :
                oauth2ErrorCode;
    }


}
