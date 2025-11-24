package com.ingot.cloud.pms.service.domain;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.TenantRoleAuthorityPrivate;
import com.ingot.cloud.pms.api.model.dto.common.BizBindDTO;
import com.ingot.framework.data.mybatis.common.service.BaseService;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author jymot
 * @since 2025-11-12
 */
public interface TenantRoleAuthorityPrivateService extends BaseService<TenantRoleAuthorityPrivate> {
    /**
     * 角色设置权限
     *
     * @param params {@link BizBindDTO}
     */
    void roleSetAuthorities(BizBindDTO params);

    /**
     * 获取角色绑定的权限ID列表
     *
     * @param id 角色ID
     * @return 权限ID列表
     */
    List<Long> getRoleBindAuthorityIds(long id);

    /**
     * 根据权限ID清理角色权限关系
     *
     * @param authorityId 权限ID
     */
    void clearByAuthorityId(long authorityId);

    /**
     * 更具角色ID清理角色权限关系
     *
     * @param roleId 角色ID
     */
    void clearByRoleId(long roleId);

    /**
     * 清空租户角色权限关系
     *
     * @param tenantId 租户ID
     */
    void clearByTenantId(long tenantId);
}
