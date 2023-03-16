package com.ingot.framework.core.model.status;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>Description  : BaseStatusCode.
 * 格式：类型+4位状态码，类型为空时为基础状态码。eg: PMS0001</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/10/10.</p>
 * <p>Time         : 2:03 下午.</p>
 */
@Getter
@RequiredArgsConstructor
public enum BaseStatusCode implements StatusCode {
    OK("S0200", "Success"),
    BAD_REQUEST("S0400", "错误的请求"),
    UNAUTHORIZED("S0401", "未授权访问"),
    FORBIDDEN("S0403", "无权访问"),
    NOT_FOUND("S0404", "找不到指定资源"),
    METHOD_NOT_ALLOWED("S0405", "请求方法不允许"),
    INTERNAL_SERVER_ERROR("S0500", "未知错误"),

    REQUEST_FALLBACK("S0001", "服务不在线, 或者网络超时"),
    ILLEGAL_OPERATION("S0002", "操作异常, %s"),
    ILLEGAL_REQUEST_PARAMS("S0003", "参数异常, %s"),
    ID_CLOCK_BACK("S1000", "时钟回拨，当前时间小于上一次操作时间");

    private final String code;
    private final String text;
}
