package com.ingot.cloud.iam.domain.organization;

import com.ingot.framework.commons.model.iam.AuthorizationDomain;

import com.ingot.framework.commons.model.iam.extension.ResourceKey;

/**
 * <p>内置成员资料的完整资源身份，注解使用同源编译期常量。</p>
 * @author jy
 * @since 1.0.0
 */
public final class MemberResources {
    /** 平台应用。 */
    public static final String PLATFORM_APPLICATION = "iam-platform";
    /** 租户应用。 */
    public static final String TENANT_APPLICATION = "iam-tenant";
    /** 成员资源。 */
    public static final String MEMBER = "member";
    /** 平台成员资料资源。 */
    public static final ResourceKey PLATFORM_MEMBER = new ResourceKey(AuthorizationDomain.PLATFORM, PLATFORM_APPLICATION, MEMBER);
    /** 租户成员资料资源。 */
    public static final ResourceKey TENANT_MEMBER = new ResourceKey(AuthorizationDomain.TENANT, TENANT_APPLICATION, MEMBER);
    private MemberResources() { }
}
