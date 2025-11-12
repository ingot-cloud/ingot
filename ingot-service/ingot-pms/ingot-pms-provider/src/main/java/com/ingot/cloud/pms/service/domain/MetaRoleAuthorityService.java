package com.ingot.cloud.pms.service.domain;

import com.ingot.cloud.pms.api.model.domain.MetaRoleAuthority;
import com.ingot.framework.data.mybatis.common.service.BaseService;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author jymot
 * @since 2025-11-12
 */
public interface MetaRoleAuthorityService extends BaseService<MetaRoleAuthority> {

    /**
     * 根据权限ID清理角色权限关系
     *
     * @param authorityId 权限ID
     */
    void clearByAuthorityId(long authorityId);
}
