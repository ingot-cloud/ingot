package com.ingot.framework.commons.model.iam;

import java.util.Map;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.ingot.framework.commons.utils.EnumUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>限定服务端候选选择器的业务用途，各用途仍需独立授权校验。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum SelectionPurpose {
    ASSIGN_RECIPIENT("ASSIGN_RECIPIENT"),
    MANAGED_DEPARTMENT("MANAGED_DEPARTMENT"),
    DIRECTORY("DIRECTORY");

    /**
     * JSON 与数据库使用的稳定字面量。
     */
    @JsonValue
    @EnumValue
    private final String value;

    private static final Map<String, SelectionPurpose> BY_VALUE = EnumUtils.index(values(), SelectionPurpose::getValue);

    /**
     * 按稳定字面量解析。
     *
     * @param value 稳定字面量；{@code null} 返回 {@code null}
     * @return 对应枚举
     * @throws IllegalArgumentException 字面量未知
     */
    @JsonCreator
    public static SelectionPurpose getEnum(String value) {
        return EnumUtils.require(BY_VALUE, value);
    }
}
