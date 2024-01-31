package com.ingot.cloud.pms.api.model.types;

/**
 * <p>Description  : UserTenantType.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2024/1/31.</p>
 * <p>Time         : 15:40.</p>
 */
public interface UserTenantType {
    Long getId();

    /**
     * 用户ID
     */
    Long getUserId();

    /**
     * 租户ID
     */
    Long getTenantId();

    /**
     * 是否为主要租户
     */
    Boolean getMain();

    /**
     * 租户名称
     */
    String getName();

    /**
     * 头像
     */
    String getAvatar();
}
