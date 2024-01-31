package com.ingot.cloud.pms.api.model.types;

import com.ingot.cloud.pms.api.model.enums.OrgTypeEnums;
import com.ingot.framework.core.model.enums.CommonStatusEnum;

/**
 * <p>Description  : AuthorityType.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2023/11/25.</p>
 * <p>Time         : 10:49.</p>
 */
public interface AuthorityType {
    Long getId();

    void setId(Long id);

    Long getPid();

    void setPid(Long pid);

    String getName();

    void setName(String name);

    String getCode();

    void setCode(String code);

    CommonStatusEnum getStatus();

    void setStatus(CommonStatusEnum status);

    OrgTypeEnums getType();

    void setType(OrgTypeEnums type);

    String getRemark();

    void setRemark(String remark);
}
