package com.ingot.cloud.pms.api.model.types;

import com.ingot.cloud.pms.api.model.enums.OrgTypeEnum;
import com.ingot.framework.core.model.enums.CommonStatusEnum;

/**
 * <p>Description  : RoleType.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/5/7.</p>
 * <p>Time         : 17:05.</p>
 */
public interface RoleType {
    /**
     * ID
     */
    Long getId();

    /**
     * 租户
     */
    Long getTenantId();

    /**
     * 模型ID
     */
    Long getModelId();

    /**
     * 组ID
     */
    Long getGroupId();

    /**
     * 角色名称
     */
    String getName();

    /**
     * 角色编码
     */
    String getCode();

    /**
     * 角色类型
     */
    OrgTypeEnum getType();

    /**
     * 状态, 0:正常，9:禁用
     */
    CommonStatusEnum getStatus();
}
