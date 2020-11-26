package com.ingot.framework.security.exception;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.ingot.framework.security.provider.IngotOAuth2ExceptionSerializer;
import org.springframework.http.HttpStatus;

/**
 * <p>Description  : ServerErrorException.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-08-22.</p>
 * <p>Time         : 14:38.</p>
 */
@JsonSerialize(using = IngotOAuth2ExceptionSerializer.class)
public class ServerErrorException extends IngotOAuth2Exception {

    public ServerErrorException(String msg, Throwable t) {
        super(msg, t);
    }

    @Override
    public String getOAuth2ErrorCode() {
        return "server_error";
    }

    @Override
    public int getHttpErrorCode() {
        return HttpStatus.INTERNAL_SERVER_ERROR.value();
    }

}