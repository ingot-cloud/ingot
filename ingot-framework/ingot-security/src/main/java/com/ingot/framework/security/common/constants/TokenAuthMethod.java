package com.ingot.framework.security.common.constants;

import lombok.Getter;

/**
 * <p>Description  : TokenAuthMethod.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/22.</p>
 * <p>Time         : 10:15 上午.</p>
 */
@Getter
public enum TokenAuthMethod {
    /**
     * 登录类型，默认标准类型，单点登录不互踢
     */
    STANDARD("0"),
    /**
     * 登录类型，唯一类型，当前账号只能在一个地方登录
     */
    UNIQUE("1");


    private String value;

    TokenAuthMethod(String value) {
        this.value = value;
    }
}
