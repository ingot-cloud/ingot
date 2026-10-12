package com.ingot.framework.commons.model.security;

import java.util.Map;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.ingot.framework.commons.utils.EnumUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>登录凭证过期的提示分类，仅选择文案，不参与登录准入。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum CredentialExpirationReason {
    /** 初始密码与管理员重置密码共用有效期和提示。 */
    INITIAL_PASSWORD_EXPIRED("INITIAL_PASSWORD_EXPIRED"),
    PASSWORD_EXPIRED("PASSWORD_EXPIRED"),
    UNKNOWN("UNKNOWN");

    /** 内部认证协议使用的稳定字面量。 */
    @JsonValue
    @EnumValue
    private final String value;

    private static final Map<String, CredentialExpirationReason> BY_VALUE =
            EnumUtils.index(values(), CredentialExpirationReason::getValue);

    /**
     * 解析内部协议字面量；未知非空值拒绝映射。
     */
    @JsonCreator
    public static CredentialExpirationReason getEnum(String value) {
        return EnumUtils.require(BY_VALUE, value);
    }
}
