package com.ingot.framework.commons.model.iam;

import java.util.Map;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.ingot.framework.commons.utils.EnumUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>
 * 定义字段输出的可见程度。限制策略按 HIDDEN、MASKED、FULL 收紧；角色正向授权按 FULL、MASKED、HIDDEN 取并集。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum FieldVisibility {

    HIDDEN("HIDDEN"), MASKED("MASKED"), FULL("FULL");

    /**
     * JSON 与数据库使用的稳定字面量。
     */
    @JsonValue
    @EnumValue
    private final String value;

    private static final Map<String, FieldVisibility> BY_VALUE = EnumUtils.index(values(), FieldVisibility::getValue);

    /**
     * 按稳定字面量解析。
     * @param value 稳定字面量；{@code null} 返回 {@code null}
     * @return 对应枚举
     * @throws IllegalArgumentException 字面量未知
     */
    @JsonCreator
    public static FieldVisibility getEnum(String value) {
        return EnumUtils.require(BY_VALUE, value);
    }

}
