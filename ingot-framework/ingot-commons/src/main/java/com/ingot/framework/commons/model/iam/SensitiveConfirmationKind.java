package com.ingot.framework.commons.model.iam;

import java.util.Map;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.ingot.framework.commons.utils.EnumUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>敏感写请求内嵌确认的种类，未实现的种类必须失败关闭。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum SensitiveConfirmationKind {
    /**
     * 当前账号登录口令。
     */
    LOGIN_PASSWORD("LOGIN_PASSWORD"),
    /**
     * 独立操作密码；本期未实现，收到即拒绝。
     */
    OPERATION_PASSWORD("OPERATION_PASSWORD");

    /**
     * JSON 与数据库使用的稳定字面量。
     */
    @JsonValue
    @EnumValue
    private final String value;

    private static final Map<String, SensitiveConfirmationKind> BY_VALUE =
            EnumUtils.index(values(), SensitiveConfirmationKind::getValue);

    /**
     * 按稳定字面量解析。
     *
     * @param value 稳定字面量；{@code null} 返回 {@code null}
     * @return 对应枚举
     * @throws IllegalArgumentException 字面量未知
     */
    @JsonCreator
    public static SensitiveConfirmationKind getEnum(String value) {
        return EnumUtils.require(BY_VALUE, value);
    }
}
