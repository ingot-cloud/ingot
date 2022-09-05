package com.ingot.framework.core.wrapper;

import com.ingot.framework.common.status.StatusCode;

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
    public <T> R<T> ok() {
        return R.ok();
    }

    /**
     * 响应成功附带 data
     */
    public <T> R<T> ok(T data) {
        return R.ok(data);
    }

    /**
     * 500 error
     */
    public <T> R<T> error() {
        return R.error500();
    }

    /**
     * 500, custom message
     */
    public <T> R<T> error(String message) {
        return R.error500(message);
    }

    /**
     * 响应失败，附带 ResponseCode
     */
    public <T> R<T> error(StatusCode code) {
        return R.error(code);
    }

    /**
     * 响应失败
     */
    public <T> R<T> error(String code, String message) {
        return R.error(code, message);
    }
}
