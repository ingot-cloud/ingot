package com.ingot.framework.core.wrapper;

import com.ingot.framework.common.status.BaseStatusCode;
import com.ingot.framework.common.status.StatusCode;
import lombok.experimental.UtilityClass;

/**
 * <p>Description  : ResponseWrapper.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/20.</p>
 * <p>Time         : 下午1:58.</p>
 */
@UtilityClass
public class ResponseWrapper {

    /**
     * 响应成功
     */
    public <T> IngotResponse<T> ok() {
        return new IngotResponse<>(BaseStatusCode.OK);
    }

    /**
     * 响应成功附带 data
     */
    public <T> IngotResponse<T> ok(T data) {
        return new IngotResponse<>(data, BaseStatusCode.OK);
    }

    /**
     * 500 error
     */
    public <T> IngotResponse<T> error500() {
        return new IngotResponse<>(BaseStatusCode.INTERNAL_SERVER_ERROR);
    }

    /**
     * 500, custom message
     */
    public <T> IngotResponse<T> error500(String message) {
        return new IngotResponse<>(BaseStatusCode.INTERNAL_SERVER_ERROR.code(), message);
    }

    /**
     * 响应失败 500，附带自定义响应体
     */
    public <T> IngotResponse<T> error500WithData(T data) {
        return new IngotResponse<>(data, BaseStatusCode.INTERNAL_SERVER_ERROR);
    }

    /**
     * 响应失败 500，附带自定义响应体
     */
    public <T> IngotResponse<T> error500WithData(T data, String message) {
        return new IngotResponse<>(data, BaseStatusCode.INTERNAL_SERVER_ERROR.code(), message);
    }

    /**
     * 响应失败，附带 ResponseCode
     */
    public <T> IngotResponse<T> error(StatusCode code) {
        return new IngotResponse<>(code);
    }

    /**
     * 响应失败
     */
    public <T> IngotResponse<T> error(String code, String message) {
        return new IngotResponse<>(code, message);
    }

    /**
     * 响应失败
     */
    public <T> IngotResponse<T> errorF(StatusCode code, Object... messages) {
        return new IngotResponse<>(code.code(), String.format(code.message(), messages));
    }

    /**
     * 响应失败，附带自定义响应体
     */
    public <T> IngotResponse<T> errorWithData(T data, StatusCode code) {
        return new IngotResponse<>(data, code);
    }

    /**
     * 响应失败
     */
    public <T> IngotResponse<T> errorWithData(T data, String code, String message) {
        return new IngotResponse<>(data, code, message);
    }
}
