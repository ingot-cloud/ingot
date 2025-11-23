package com.ingot.cloud.pms.service.domain;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.MetaRoleAuthority;
import com.ingot.framework.commons.model.common.AssignDTO;
import com.ingot.framework.commons.model.common.SetDTO;
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
     * 角色设置权限
     *
     * @param params {@link AssignDTO}
     */
    void roleSetAuthorities(SetDTO<Long, Long> params);

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
}
