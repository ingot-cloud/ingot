package com.ingot.framework.core.wrapper;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ingot.framework.common.status.BaseStatusCode;
import com.ingot.framework.common.status.StatusCode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.ToString;

import java.io.Serializable;

/**
 * <p>Description  : R.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/20.</p>
 * <p>Time         : 下午2:01.</p>
 */
@ToString
@ApiModel(description = "响应信息主体")
public class R<T> implements Serializable {
    /**
     * 响应码字段
     */
    public static final String CODE = "code";
    /**
     * 响应数据字段
     */
    public static final String DATA = "data";
    /**
     * 响应消息字段
     */
    public static final String MESSAGE = "message";

    /**
     * 响应码
     */
    @ApiModelProperty(value = "code：成功=0200")
    private String code = BaseStatusCode.OK.code();
    /**
     * 消息
     */
    @ApiModelProperty(value = "消息")
    private String message;
    /**
     * 响应体
     */
    @ApiModelProperty(value = "数据")
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

    public R(StatusCode code) {
        this(code.code(), code.message());
    }

    public R(T data, StatusCode code) {
        this(code);
        this.data = data;
    }

    public R<?> data(T data) {
        this.data = data;
        return this;
    }

    public T getData() {
        return data;
    }

    public String getCode() {
        return code;
    }

    public R<?> code(String code) {
        this.code = code;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public R<?> message(String message) {
        this.message = message;
        return this;
    }

    /**
     * 请求是否成功
     */
    @JsonIgnore
    public boolean isSuccess() {
        return StrUtil.equals(getCode(), BaseStatusCode.OK.code());
    }

    /**
     * 响应成功
     */
    public static <T> R<T> ok() {
        return new R<>(BaseStatusCode.OK);
    }

    /**
     * 响应成功附带 data
     */
    public static <T> R<T> ok(T data) {
        return new R<>(data, BaseStatusCode.OK);
    }

    /**
     * 500 error
     */
    public static <T> R<T> error500() {
        return new R<>(BaseStatusCode.INTERNAL_SERVER_ERROR);
    }

    /**
     * 500, custom message
     */
    public static <T> R<T> error500(String message) {
        return new R<>(BaseStatusCode.INTERNAL_SERVER_ERROR.code(), message);
    }

    /**
     * 响应失败 500，附带自定义响应体
     */
    public static <T> R<T> error500WithData(T data) {
        return new R<>(data, BaseStatusCode.INTERNAL_SERVER_ERROR);
    }

    /**
     * 响应失败 500，附带自定义响应体
     */
    public static <T> R<T> error500WithData(T data, String message) {
        return new R<>(data, BaseStatusCode.INTERNAL_SERVER_ERROR.code(), message);
    }

    /**
     * 响应失败，附带 ResponseCode
     */
    public static <T> R<T> error(StatusCode code) {
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
    public static <T> R<T> errorF(StatusCode code, Object... messages) {
        return new R<>(code.code(), String.format(code.message(), messages));
    }

    /**
     * 响应失败，附带自定义响应体
     */
    public static <T> R<T> errorWithData(T data, StatusCode code) {
        return new R<>(data, code);
    }

    /**
     * 响应失败
     */
    public static <T> R<T> errorWithData(T data, String code, String message) {
        return new R<>(data, code, message);
    }
}
