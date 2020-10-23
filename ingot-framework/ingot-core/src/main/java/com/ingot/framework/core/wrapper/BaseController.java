package com.ingot.framework.core.wrapper;

import com.ingot.framework.base.status.StatusCode;

/**
 * <p>Description  : BaseController.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/10/19.</p>
 * <p>Time         : 10:02 上午.</p>
 */
public class BaseController {
    /**
     * 响应成功
     */
    public <T> IngotResponse<T> ok(){
        return ResponseWrapper.ok();
    }

    /**
     * 响应成功附带 data
     */
    public <T> IngotResponse<T> ok(T data){
        return ResponseWrapper.ok(data);
    }

    /**
     * 500 error
     */
    public <T> IngotResponse<T> error(){
        return ResponseWrapper.error();
    }

    /**
     * 500, custom message
     */
    public <T> IngotResponse<T> error(String message){
        return ResponseWrapper.error(message);
    }

    /**
     * 响应失败，附带 ResponseCode
     */
    public <T> IngotResponse<T> error(StatusCode code){
        return ResponseWrapper.error(code);
    }

    /**
     * 响应失败
     */
    public <T> IngotResponse<T> error(String code, String message) {
        return ResponseWrapper.error(code, message);
    }
}