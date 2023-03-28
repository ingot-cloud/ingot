package com.ingot.framework.vc;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>Description  : VerificationCodeType.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/3/28.</p>
 * <p>Time         : 11:49 PM.</p>
 */
@Getter
@RequiredArgsConstructor
public enum VerificationCodeType {

    SMS("1", "短信验证码"),
    EMAIL("2", "邮箱验证码"),
    IMAGE("3", "图形验证码");

    private final String value;
    private final String text;
}
