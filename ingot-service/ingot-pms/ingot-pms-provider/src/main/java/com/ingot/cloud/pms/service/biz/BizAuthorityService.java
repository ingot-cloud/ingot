package com.ingot.cloud.pms.service.biz;

import java.util.List;

import com.ingot.cloud.pms.api.model.types.AuthorityType;
import com.ingot.cloud.pms.api.model.types.RoleType;

/**
 * <p>Description  : BizAuthorityService.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/18.</p>
 * <p>Time         : 11:49.</p>
 */
public interface BizAuthorityService {

    /**
     * 根据角色获取权限
     *
     * @param roles 角色列表
     * @return 权限列表
     */
    List<AuthorityType> getAuthoritiesByRoleIds(List<RoleType> roles);

    /**
     * 根据角色列表获取权限及子权限
     *
     * @param roles 角色列表
     * @return 权限列表
     */
    List<AuthorityType> getAuthoritiesAndChildrenByRoleIds(List<RoleType> roles);
}
