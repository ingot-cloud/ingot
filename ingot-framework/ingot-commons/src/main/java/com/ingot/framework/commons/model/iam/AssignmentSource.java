package com.ingot.framework.commons.model.iam;

import java.util.Map;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.ingot.framework.commons.utils.EnumUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>区分角色分配的写入来源，来源不替代主体和委派校验。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum AssignmentSource {
    MANUAL("MANUAL"),
    INITIALIZATION("INITIALIZATION"),
    MIGRATION("MIGRATION");

    /**
     * JSON 与数据库使用的稳定字面量。
     */
    @JsonValue
    @EnumValue
    private final String value;

    private static final Map<String, AssignmentSource> BY_VALUE = EnumUtils.index(values(), AssignmentSource::getValue);

    /**
     * 按稳定字面量解析。
     *
     * @param value 稳定字面量；{@code null} 返回 {@code null}
     * @return 对应枚举
     * @throws IllegalArgumentException 字面量未知
     */
    @JsonCreator
    public static AssignmentSource getEnum(String value) {
        return EnumUtils.require(BY_VALUE, value);
    }
}
