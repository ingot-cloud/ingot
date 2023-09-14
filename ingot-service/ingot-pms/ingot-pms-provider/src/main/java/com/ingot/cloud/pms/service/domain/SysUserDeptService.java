package com.ingot.cloud.pms.service.domain;

import com.ingot.cloud.pms.api.model.domain.SysUserDept;
import com.ingot.framework.data.mybatis.service.BaseService;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author jymot
 * @since 2023-09-13
 */
public interface SysUserDeptService extends BaseService<SysUserDept> {

    /**
     * 根据用户ID和租户ID获取{@link SysUserDept}
     *
     * @param userId   用户ID
     * @param tenantId 租户ID
     * @return {@link SysUserDept}
     */
    SysUserDept getByUserIdAndTenant(long userId, long tenantId);
}
