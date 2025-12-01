package com.ingot.cloud.member.service.domain;

import java.util.List;

import com.ingot.cloud.member.api.model.domain.MemberUserTenant;
import com.ingot.framework.data.mybatis.common.service.BaseService;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author jymot
 * @since 2025-11-29
 */
public interface MemberUserTenantService extends BaseService<MemberUserTenant> {
    /**
     * 加入租户
     *
     * @param userId   用户ID
     * @param tenantId 要加入的租户
     */
    void joinTenant(long userId, long tenantId);

    /**
     * 离开租户
     *
     * @param userId 用户ID
     */
    void leaveTenant(long userId);

    /**
     * 根据组织ID清理关联关系
     *
     * @param tenantId 组织ID
     */
    void clearByTenantId(long tenantId);

    /**
     * 更新基本信息
     *
     * @param tenant 租户ID
     */
    void updateBase(long tenant);

    /**
     * 获取用户组织
     *
     * @param userId 用户ID
     * @return {@link MemberUserTenant}
     */
    List<MemberUserTenant> getUserOrgs(long userId);
}
