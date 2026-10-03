package com.ingot.cloud.iam.assignment;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.ingot.framework.commons.utils.EnumUtils;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>已登记真实目标标识查询的租户资源，不从请求参数选择表或列。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum TenantScopeObjectResource {
    APPLICATION("application"),
    ASSIGNMENT("assignment"),
    DELEGATION("delegation"),
    DEPARTMENT("department"),
    DIRECTORY("directory"),
    GROUP("group"),
    MEMBER("member"),
    ROLE("role");

    /** 持久化与目录使用的稳定资源编码。 */
    @JsonValue
    @EnumValue
    private final String value;

    private static final Map<String, TenantScopeObjectResource> BY_VALUE =
            EnumUtils.index(values(), TenantScopeObjectResource::getValue);

    /**
     * 解析稳定资源编码。
     * @param value 资源编码
     * @return 资源适配器
     */
    @JsonCreator
    public static TenantScopeObjectResource getEnum(String value) {
        return EnumUtils.require(BY_VALUE, value);
    }

    /**
     * 查找已登记资源；未知返回 null。
     * @param value 资源编码
     * @return 资源适配器或 null
     */
    public static TenantScopeObjectResource find(String value) {
        return BY_VALUE.get(value);
    }
}
