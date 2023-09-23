package com.ingot.cloud.pms.service.domain;

import com.ingot.cloud.pms.api.model.domain.SysUserTenant;
import com.ingot.framework.data.mybatis.service.BaseService;

/**
 * <p>Description  : SysUserTenantService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/2.</p>
 * <p>Time         : 11:44 AM.</p>
 */
public interface SysUserTenantService extends BaseService<SysUserTenant> {

    /**
     * 加入租户
     * @param userId 用户ID
     */
    void joinTenant(long userId);

    /**
     * 离开租户
     * @param userId 用户ID
     */
    void leaveTenant(long userId);
}
