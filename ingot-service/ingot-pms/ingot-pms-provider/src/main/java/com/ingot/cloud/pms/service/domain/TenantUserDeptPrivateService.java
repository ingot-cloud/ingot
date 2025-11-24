package com.ingot.cloud.pms.service.domain;

import java.util.Collection;
import java.util.List;

import com.ingot.cloud.pms.api.model.domain.TenantUserDeptPrivate;
import com.ingot.framework.data.mybatis.common.service.BaseService;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author jymot
 * @since 2025-11-17
 */
public interface TenantUserDeptPrivateService extends BaseService<TenantUserDeptPrivate> {

    /**
     * 设置部门
     *
     * @param userId  用户ID
     * @param deptIds 部门ID列表
     */
    void setDepartments(long userId, Collection<Long> deptIds);

    /**
     * 获取用户部门ID列表
     *
     * @param userId 用户ID
     * @return List
     */
    List<Long> getUserDepartmentIds(long userId);

    /**
     * 根据用户ID清空数据
     *
     * @param userId 用户ID
     */
    void clearByUserId(long userId);

    /**
     * 根据租户ID清理数据
     * @param tenantId 租户ID
     */
    void clearByTenantId(long tenantId);
}
