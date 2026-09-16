package com.ingot.framework.commons.model.iam;

import java.util.Map;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.ingot.framework.commons.utils.EnumUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>标识系统、共享及两域自定义角色的来源，定制租户版本通过基础版本引用区分。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum RoleKind {
    SYSTEM("SYSTEM"),
    SHARED("SHARED"),
    PLATFORM_CUSTOM("PLATFORM_CUSTOM"),
    TENANT_CUSTOM("TENANT_CUSTOM");

    /**
     * JSON 与数据库使用的稳定字面量。
     */
    @JsonValue
    @EnumValue
    private final String value;

    private static final Map<String, RoleKind> BY_VALUE = EnumUtils.index(values(), RoleKind::getValue);

    /**
     * 按稳定字面量解析。
     *
     * @param value 稳定字面量；{@code null} 返回 {@code null}
     * @return 对应枚举
     * @throws IllegalArgumentException 字面量未知
     */
    @JsonCreator
    public static RoleKind getEnum(String value) {
        return EnumUtils.require(BY_VALUE, value);
    }
}
