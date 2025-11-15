package com.ingot.cloud.pms.service.domain;

import com.ingot.cloud.pms.api.model.domain.TenantRoleUserPrivate;
import com.ingot.framework.data.mybatis.common.service.BaseService;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author jymot
 * @since 2025-11-12
 */
public interface TenantRoleUserPrivateService extends BaseService<TenantRoleUserPrivate> {

    /**
     * 根据角色ID清空角色用户关系
     *
     * @param id 角色ID
     */
    void clearByRoleId(long id);
}
