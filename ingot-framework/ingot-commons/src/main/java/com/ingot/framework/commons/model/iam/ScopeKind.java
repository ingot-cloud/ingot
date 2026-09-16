package com.ingot.framework.commons.model.iam;

import java.util.Map;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.ingot.framework.commons.utils.EnumUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>定义 IAM 操作授权支持的范围表达式种类，具体资源仍须校验自身支持的能力。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum ScopeKind {
    ALL("ALL"),
    SELF("SELF"),
    MEMBER_DEPARTMENTS("MEMBER_DEPARTMENTS"),
    MANAGED_DEPARTMENTS("MANAGED_DEPARTMENTS"),
    OBJECT_SET("OBJECT_SET");

    /**
     * JSON 与数据库使用的稳定字面量。
     */
    @JsonValue
    @EnumValue
    private final String value;

    private static final Map<String, ScopeKind> BY_VALUE = EnumUtils.index(values(), ScopeKind::getValue);

    /**
     * 按稳定字面量解析。
     *
     * @param value 稳定字面量；{@code null} 返回 {@code null}
     * @return 对应枚举
     * @throws IllegalArgumentException 字面量未知
     */
    @JsonCreator
    public static ScopeKind getEnum(String value) {
        return EnumUtils.require(BY_VALUE, value);
    }
}
