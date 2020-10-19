package com.ingot.common.core.wrapper;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ingot.common.base.status.BaseStatusCode;
import com.ingot.common.base.status.StatusCode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.ToString;

import java.io.Serializable;

/**
 * <p>Description  : IngotResponse.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/20.</p>
 * <p>Time         : 下午2:01.</p>
 */
@ToString
@ApiModel(description = "响应信息主体")
public class IngotResponse<T> implements Serializable {
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

    public IngotResponse() {
    }

    public IngotResponse(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public IngotResponse(T data, String code, String message) {
        this(code, message);
        this.data = data;
    }

    public IngotResponse(StatusCode code) {
        this(code.code(), code.message());
    }

    public IngotResponse(T data, StatusCode code) {
        this(code);
        this.data = data;
    }

    public IngotResponse<?> data(T data) {
        this.data = data;
        return this;
    }

    public T getData() {
        return data;
    }

    public String getCode() {
        return code;
    }

    public IngotResponse<?> code(String code) {
        this.code = code;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public IngotResponse<?> message(String message) {
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
}
