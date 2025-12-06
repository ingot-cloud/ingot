package com.ingot.cloud.member.service.biz;

import com.ingot.framework.commons.model.common.TenantBaseDTO;

/**
 * <p>Description  : BizTenantService.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/6.</p>
 * <p>Time         : 08:45.</p>
 */
public interface BizTenantService {

    /**
     * 删除租户
     *
     * @param id 租户ID
     */
    void deleteTenant(long id);

    /**
     * 更新租户信息
     *
     * @param params {@link TenantBaseDTO}
     */
    void updateBase(TenantBaseDTO params);
}
