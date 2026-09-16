package com.ingot.framework.commons.model.iam;

import java.util.Map;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.ingot.framework.commons.utils.EnumUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>定义租户对固定共享角色版本的单操作差异，不表达跨角色的全局拒绝。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum RoleDeltaOperation {
    ADD("ADD"),
    REMOVE("REMOVE"),
    REPLACE_SCOPE("REPLACE_SCOPE");

    /**
     * JSON 与数据库使用的稳定字面量。
     */
    @JsonValue
    @EnumValue
    private final String value;

    private static final Map<String, RoleDeltaOperation> BY_VALUE = EnumUtils.index(values(), RoleDeltaOperation::getValue);

    /**
     * 按稳定字面量解析。
     *
     * @param value 稳定字面量；{@code null} 返回 {@code null}
     * @return 对应枚举
     * @throws IllegalArgumentException 字面量未知
     */
    @JsonCreator
    public static RoleDeltaOperation getEnum(String value) {
        return EnumUtils.require(BY_VALUE, value);
    }
}
