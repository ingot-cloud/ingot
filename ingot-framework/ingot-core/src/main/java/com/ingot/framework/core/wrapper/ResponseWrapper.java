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
    public <T> R<T> ok() {
        return new R<>(BaseStatusCode.OK);
    }

    /**
     * 响应成功附带 data
     */
    public <T> R<T> ok(T data) {
        return new R<>(data, BaseStatusCode.OK);
    }

    /**
     * 500 error
     */
    public <T> R<T> error500() {
        return new R<>(BaseStatusCode.INTERNAL_SERVER_ERROR);
    }

    /**
     * 500, custom message
     */
    public <T> R<T> error500(String message) {
        return new R<>(BaseStatusCode.INTERNAL_SERVER_ERROR.code(), message);
    }

    /**
     * 响应失败 500，附带自定义响应体
     */
    public <T> R<T> error500WithData(T data) {
        return new R<>(data, BaseStatusCode.INTERNAL_SERVER_ERROR);
    }

    /**
     * 响应失败 500，附带自定义响应体
     */
    public <T> R<T> error500WithData(T data, String message) {
        return new R<>(data, BaseStatusCode.INTERNAL_SERVER_ERROR.code(), message);
    }

    /**
     * 响应失败，附带 ResponseCode
     */
    public <T> R<T> error(StatusCode code) {
        return new R<>(code);
    }

    /**
     * 响应失败
     */
    public <T> R<T> error(String code, String message) {
        return new R<>(code, message);
    }

    /**
     * 响应失败
     */
    public <T> R<T> errorF(StatusCode code, Object... messages) {
        return new R<>(code.code(), String.format(code.message(), messages));
    }

    /**
     * 响应失败，附带自定义响应体
     */
    public <T> R<T> errorWithData(T data, StatusCode code) {
        return new R<>(data, code);
    }

    /**
     * 响应失败
     */
    public <T> R<T> errorWithData(T data, String code, String message) {
        return new R<>(data, code, message);
    }
}
