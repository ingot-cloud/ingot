package com.ingot.framework.core.status;

import com.ingot.framework.common.status.StatusCode;

/**
 * <p>Description  : CoreStatusCode.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/10/19.</p>
 * <p>Time         : 9:34 上午.</p>
 */
public enum CoreStatusCode implements StatusCode {
    PRECONDITION_BEANS("C0001", "预校验类中没有相关校验方法"),
    PRECONDITION_NO_SUCH_METHOD("C0002", "无法访问预校验中的校验方法"),
    PRECONDITION_ILLEGAL_ACCESS("C0003", "无法注入指定的预校验类");

    private final String code;
    private final String message;

    CoreStatusCode(String code, String message){
        this.code = code;
        this.message = message;
    }

    @Override public String code() {
        return code;
    }

    @Override public String message() {
        return message;
    }
}
