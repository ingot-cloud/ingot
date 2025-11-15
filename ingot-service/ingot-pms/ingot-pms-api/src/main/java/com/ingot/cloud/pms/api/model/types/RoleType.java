package com.ingot.cloud.pms.api.model.types;

import java.util.List;

import com.ingot.cloud.pms.api.model.enums.OrgTypeEnum;
import com.ingot.cloud.pms.api.model.enums.RoleTypeEnum;
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
    OrgTypeEnum getOrgType();

    void setOrgType(OrgTypeEnum orgType);

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
