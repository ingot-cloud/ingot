package com.ingot.cloud.pms.service.domain;

import com.ingot.cloud.pms.api.model.domain.AppUserTenant;
import com.ingot.cloud.pms.api.model.domain.SysTenant;
import com.ingot.framework.data.mybatis.service.BaseService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author jymot
 * @since 2023-09-12
 */
public interface AppUserTenantService extends BaseService<AppUserTenant> {
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
     * @return {@link AppUserTenant}
     */
    List<AppUserTenant> getUserOrgs(long userId);
}
