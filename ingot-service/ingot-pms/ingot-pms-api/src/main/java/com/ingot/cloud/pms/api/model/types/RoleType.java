package com.ingot.cloud.pms.api.model.types;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.TenantRolePrivate;
import com.ingot.cloud.pms.api.model.enums.OrgTypeEnum;
import com.ingot.cloud.pms.api.model.enums.RoleTypeEnum;
import com.ingot.framework.commons.constants.IDConstants;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.data.mybatis.common.model.DataScopeTypeEnum;

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

    void setId(Long id);

    /**
     * PID
     */
    Long getPid();

    void setPid(Long id);

    /**
     * 获取角色对应租户ID，只对{@link TenantRolePrivate}生效
     */
    default Long getTenantId() {
        return IDConstants.DEFAULT_TENANT_ID;
    }

    /**
     * 角色名称
     */
    String getName();

    void setName(String name);

    /**
     * 角色编码
     */
    String getCode();

    void setCode(String code);

    /**
     * 角色类型
     */
    RoleTypeEnum getType();

    void setType(RoleTypeEnum type);

    /**
     * 组织类型
     */
    default OrgTypeEnum getOrgType() {
        return OrgTypeEnum.Tenant;
    }

    default void setOrgType(OrgTypeEnum orgType) {
        // nothing to do
    }

    /**
     * 域类型
     */
    DataScopeTypeEnum getScopeType();

    void setScopeType(DataScopeTypeEnum scopeType);

    /**
     * Scopes
     */
    List<Long> getScopes();

    void setScopes(List<Long> scopes);

    /**
     * 状态, 0:正常，9:禁用
     */
    CommonStatusEnum getStatus();

    void setStatus(CommonStatusEnum status);
}
