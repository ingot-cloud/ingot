package com.ingot.framework.core.error.exception;

import com.ingot.framework.core.model.status.ErrorCode;

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

    public BizException(ErrorCode errorCode){
        super(errorCode.getText());
        this.code = errorCode.getCode();
    }

    public BizException(ErrorCode responseCode, Object... args){
        super(String.format(responseCode.getText(), args));
        this.code = responseCode.getCode();
    }

    public String getCode(){
        return code;
    }
}
