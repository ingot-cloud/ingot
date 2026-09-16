package com.ingot.framework.commons.model.iam;

import java.util.Map;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.ingot.framework.commons.utils.EnumUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>标识成员资料字段策略匹配键，禁止在调用点裸写字段名。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum MemberFieldKey {
    /**
     * 显示名。
     */
    DISPLAY_NAME("displayName"),
    /**
     * 头像。
     */
    AVATAR("avatar"),
    /**
     * 组织通讯录手机号，不是全局登录手机号。
     */
    PHONE("phone"),
    /**
     * 组织通讯录邮箱，不是全局登录邮箱。
     */
    EMAIL("email");

    /**
     * 契约字面量 {@code displayName}。
     */
    public static final String VALUE_DISPLAY_NAME = "displayName";
    /**
     * 契约字面量 {@code avatar}。
     */
    public static final String VALUE_AVATAR = "avatar";
    /**
     * 契约字面量 {@code phone}。
     */
    public static final String VALUE_PHONE = "phone";
    /**
     * 契约字面量 {@code email}。
     */
    public static final String VALUE_EMAIL = "email";

    /**
     * JSON 与字段策略使用的稳定键。
     */
    @JsonValue
    @EnumValue
    private final String value;

    private static final Map<String, MemberFieldKey> BY_VALUE =
            EnumUtils.index(values(), MemberFieldKey::getValue);

    /**
     * 按字段策略稳定键解析。
     *
     * @param value 字段键；{@code null} 返回 {@code null}
     * @return 对应枚举
     * @throws IllegalArgumentException 字面量未知
     */
    @JsonCreator
    public static MemberFieldKey getEnum(String value) {
        return EnumUtils.require(BY_VALUE, value);
    }
}
