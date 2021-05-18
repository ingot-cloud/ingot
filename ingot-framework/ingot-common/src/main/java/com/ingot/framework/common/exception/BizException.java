package com.ingot.framework.common.exception;

import com.ingot.framework.common.status.StatusCode;

/**
 * <p>Description  : 业务异常.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/10/9.</p>
 * <p>Time         : 3:41 下午.</p>
 */
public class BizException extends RuntimeException {
    private final String code;

    public BizException(String code, String message){
        super(message);
        this.code = code;
    }

    public BizException(String code, String message, Object... args){
        super(String.format(message, args));
        this.code = code;
    }

    public BizException(StatusCode statusCode){
        super(statusCode.message());
        this.code = statusCode.code();
    }

    public BizException(StatusCode responseCode, Object... args){
        super(String.format(responseCode.message(), args));
        this.code = responseCode.code();
    }

    public String getCode(){
        return code;
    }
}
