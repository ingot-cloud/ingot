package com.ingot.framework.commons.model.support;

import com.ingot.framework.commons.model.status.ErrorCode;

/**
 * <p>Description  : {@link R}快捷方法.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/10/19.</p>
 * <p>Time         : 10:02 上午.</p>
 */
public interface RShortcuts {
    /**
     * 响应成功
     */
    default <T> R<T> ok() {
        return R.ok();
    }

    /**
     * 响应成功附带 data
     */
    default <T> R<T> ok(T data) {
        return R.ok(data);
    }

    /**
     * 500 error
     */
    default <T> R<T> error() {
        return R.error500();
    }

    /**
     * 500, custom message
     */
    default <T> R<T> error(String message) {
        return R.error500(message);
    }

    /**
     * 响应失败，附带 ResponseCode
     */
    default <T> R<T> error(ErrorCode code) {
        return R.error(code);
    }

    /**
     * 响应失败
     */
    default <T> R<T> error(String code, String message) {
        return R.error(code, message);
    }
}
