package com.ingot.framework.core.model.support;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.core.model.status.BaseErrorCode;
import com.ingot.framework.core.model.status.ErrorCode;
import lombok.ToString;

import java.io.Serializable;
import java.util.function.Consumer;

/**
 * <p>Description  : R.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/20.</p>
 * <p>Time         : 下午2:01.</p>
 */
@ToString
public class R<T> implements Serializable {
    /**
     * 错误码字段
     */
    public static final String CODE = "code";
    /**
     * 响应数据字段
     */
    public static final String DATA = "data";
    /**
     * 错误消息字段
     */
    public static final String MESSAGE = "message";

    /**
     * 响应码
     */
    private String code = BaseErrorCode.OK.getCode();
    /**
     * 消息
     */
    private String message;
    /**
     * 响应体
     */
    private T data;

    public R() {
    }

    public R(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public R(T data, String code, String message) {
        this(code, message);
        this.data = data;
    }

    public R(ErrorCode code) {
        this(code.getCode(), code.getText());
    }

    public R(T data, ErrorCode code) {
        this(code);
        this.data = data;
    }

    public R<T> data(T data) {
        this.data = data;
        return this;
    }

    public T getData() {
        return data;
    }

    public String getCode() {
        return code;
    }

    public R<T> code(String code) {
        this.code = code;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public R<T> message(String message) {
        this.message = message;
        return this;
    }

    /**
     * 请求是否成功
     */
    public boolean isSuccess() {
        return StrUtil.equals(getCode(), BaseErrorCode.OK.getCode());
    }

    /**
     * 如果成功则Consumer，并且传递Data
     *
     * @param consumer {@link Consumer}
     * @return this
     */
    public R<T> ifSuccess(Consumer<T> consumer) {
        if (isSuccess()) {
            consumer.accept(data);
        }
        return this;
    }


    // 实例方法
    // =============================================================

    /**
     * 响应成功
     */
    public static <T> R<T> ok() {
        return new R<>(BaseErrorCode.OK);
    }

    /**
     * 响应成功附带 data
     */
    public static <T> R<T> ok(T data) {
        return new R<>(data, BaseErrorCode.OK);
    }

    /**
     * 500 error
     */
    public static <T> R<T> error500() {
        return new R<>(BaseErrorCode.INTERNAL_SERVER_ERROR);
    }

    /**
     * 500, custom message
     */
    public static <T> R<T> error500(String message) {
        return new R<>(BaseErrorCode.INTERNAL_SERVER_ERROR.getCode(), message);
    }

    /**
     * 响应失败 500，附带自定义响应体
     */
    public static <T> R<T> error500(T data) {
        return new R<>(data, BaseErrorCode.INTERNAL_SERVER_ERROR);
    }

    /**
     * 响应失败 500，附带自定义响应体
     */
    public static <T> R<T> error500(T data, String message) {
        return new R<>(data, BaseErrorCode.INTERNAL_SERVER_ERROR.getCode(), message);
    }

    /**
     * 响应失败，附带 ResponseCode
     */
    public static <T> R<T> error(ErrorCode code) {
        return new R<>(code);
    }

    /**
     * 响应失败
     */
    public static <T> R<T> error(String code, String message) {
        return new R<>(code, message);
    }

    /**
     * 响应失败
     */
    public static <T> R<T> errorF(ErrorCode code, Object... messages) {
        return new R<>(code.getCode(), String.format(code.getText(), messages));
    }

    /**
     * 响应失败，附带自定义响应体
     */
    public static <T> R<T> error(T data, ErrorCode code) {
        return new R<>(data, code);
    }

    /**
     * 响应失败
     */
    public static <T> R<T> error(T data, String code, String message) {
        return new R<>(data, code, message);
    }
}
