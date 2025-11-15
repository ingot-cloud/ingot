package com.ingot.cloud.pms.api.model.types;

import com.ingot.cloud.pms.api.model.enums.AuthorityTypeEnum;
import com.ingot.cloud.pms.api.model.enums.OrgTypeEnum;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;

/**
 * <p>Description  : AuthorityType.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2023/11/25.</p>
 * <p>Time         : 10:49.</p>
 */
public interface AuthorityType {
    /**
     * ID
     */
    Long getId();

    void setId(Long id);

    /**
     * PID
     */
    Long getPid();

    void setPid(Long pid);

    /**
     * 权限名称
     */
    String getName();

    void setName(String name);

    /**
     * 权限编码
     */
    String getCode();

    void setCode(String code);

    /**
     * 权限类型
     */
    AuthorityTypeEnum getType();

    void setType(AuthorityTypeEnum type);

    /**
     * 组织类型
     */
    OrgTypeEnum getOrgType();

    void setOrgType(OrgTypeEnum type);

    /**
     * 状态, 0:正常，9:禁用
     */
    CommonStatusEnum getStatus();

    void setStatus(CommonStatusEnum status);

    /**
     * 备注
     */
    String getRemark();

    void setRemark(String remark);
}
