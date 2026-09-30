package com.ingot.cloud.iam.assignment;

import java.util.Map;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.ingot.framework.commons.utils.EnumUtils;
import com.ingot.framework.commons.model.iam.IamAction;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>已接入实际业务对象查询的平台资源编码。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum PlatformScopeObjectResource {
    MEMBER("member", IamAction.PLATFORM_MEMBER_READ),
    GROUP("group", IamAction.PLATFORM_GROUP_READ),
    TENANT("tenant", IamAction.PLATFORM_TENANT_READ),
    ACCOUNT("account", IamAction.PLATFORM_ACCOUNT_READ),
    ENTITLEMENT("entitlement", IamAction.PLATFORM_ENTITLEMENT_READ),
    APPLICATION("application", IamAction.PLATFORM_APPLICATION_READ),
    RESOURCE("resource", IamAction.PLATFORM_RESOURCE_READ),
    ACTION("action", IamAction.PLATFORM_ACTION_READ),
    MENU("menu", IamAction.PLATFORM_MENU_READ),
    ROLE("role", IamAction.PLATFORM_ROLE_READ),
    SHARED_ROLE("shared-role", IamAction.PLATFORM_SHARED_ROLE_READ),
    PLAN("plan", IamAction.PLATFORM_PLAN_READ),
    ASSIGNMENT("assignment", IamAction.PLATFORM_ASSIGNMENT_READ),
    DELEGATION("delegation", IamAction.PLATFORM_DELEGATION_READ);

    /** JSON 与持久化使用的稳定资源编码。 */
    @JsonValue
    @EnumValue
    private final String value;

    /** 诊断时披露该资源对象所需的读取操作。 */
    private final IamAction readAction;

    private static final Map<String, PlatformScopeObjectResource> BY_VALUE =
            EnumUtils.index(values(), PlatformScopeObjectResource::getValue);

    /**
     * 解析稳定资源编码。
     * @param value 资源编码；null 返回 null
     * @return 对应适配器
     * @throws IllegalArgumentException 未登记编码
     */
    @JsonCreator
    public static PlatformScopeObjectResource getEnum(String value) {
        return EnumUtils.require(BY_VALUE, value);
    }

    /**
     * 查找已登记适配器，供未接入资源返回明确的不支持状态。
     * @param value 资源编码
     * @return 已登记适配器；未接入时为 null
     */
    public static PlatformScopeObjectResource find(String value) {
        return BY_VALUE.get(value);
    }
}
