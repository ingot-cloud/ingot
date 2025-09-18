package com.ingot.cloud.pms.service.domain;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.SysTenant;
import com.ingot.cloud.pms.api.model.domain.SysUserTenant;
import com.ingot.framework.data.mybatis.common.service.BaseService;

/**
 * <p>Description  : SysUserTenantService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/2.</p>
 * <p>Time         : 11:44 AM.</p>
 */
public interface SysUserTenantService extends BaseService<SysUserTenant> {

    /**
     * 加入租户
     *
     * @param userId 用户ID
     * @param tenant 要加入的租户
     */
    void joinTenant(long userId, SysTenant tenant);

    /**
     * 离开租户
     *
     * @param userId 用户ID
     */
    void leaveTenant(long userId);

    /**
     * 更新基本信息
     *
     * @param params {@link SysTenant}
     */
    void updateBase(SysTenant params);

    /**
     * 获取用户组织
     *
     * @param userId 用户ID
     * @return {@link SysUserTenant}
     */
    List<SysUserTenant> getUserOrgs(long userId);
}
