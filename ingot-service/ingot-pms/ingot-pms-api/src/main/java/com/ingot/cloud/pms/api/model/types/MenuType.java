package com.ingot.cloud.pms.api.model.types;

import com.ingot.cloud.pms.api.model.enums.OrgTypeEnum;

/**
 * <p>Description  : MenuType.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/13.</p>
 * <p>Time         : 17:12.</p>
 */
public interface MenuType {

    void setName(String name);

    String getName();

    void setOrgType(OrgTypeEnum orgType);

    OrgTypeEnum getOrgType();
}
