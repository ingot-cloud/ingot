package com.ingot.framework.commons.model.iam;

import java.util.Map;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.ingot.framework.commons.utils.EnumUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>分配计算后的生效状态。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum AssignmentEffectiveStatus {
    PENDING("PENDING"),
    ACTIVE("ACTIVE"),
    EXPIRED("EXPIRED"),
    REVOKED("REVOKED"),
    SOURCE_INVALID("SOURCE_INVALID");

    /**
     * JSON 与数据库使用的稳定字面量。
     */
    @JsonValue
    @EnumValue
    private final String value;

    private static final Map<String, AssignmentEffectiveStatus> BY_VALUE = EnumUtils.index(values(), AssignmentEffectiveStatus::getValue);

    /**
     * 按稳定字面量解析。
     *
     * @param value 稳定字面量；{@code null} 返回 {@code null}
     * @return 对应枚举
     * @throws IllegalArgumentException 字面量未知
     */
    @JsonCreator
    public static AssignmentEffectiveStatus getEnum(String value) {
        return EnumUtils.require(BY_VALUE, value);
    }
}
