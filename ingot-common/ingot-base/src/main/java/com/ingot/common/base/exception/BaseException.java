package com.ingot.common.base.exception;

import com.ingot.common.base.http.StatusCode;

/**
 * <p>Description  : BaseException.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/10/9.</p>
 * <p>Time         : 3:41 下午.</p>
 */
public class BaseException extends RuntimeException {
    private final String code;

    public BaseException(String code, String message){
        super(message);
        this.code = code;
    }

    public BaseException(String code, String message, Object... args){
        super(String.format(message, args));
        this.code = code;
    }

    public BaseException(StatusCode statusCode){
        super(statusCode.message());
        this.code = statusCode.code();
    }

    public BaseException(StatusCode responseCode, Object... args){
        super(String.format(responseCode.message(), args));
        this.code = responseCode.code();
    }

    public String getCode(){
        return code;
    }
}
