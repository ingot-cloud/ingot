package com.ingot.cloud.pms.service.biz;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.AppRole;
import com.ingot.cloud.pms.api.model.domain.AppRoleGroup;

/**
 * <p>Description  : BizAppRoleService.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2024/1/24.</p>
 * <p>Time         : 09:30.</p>
 */
public interface BizAppRoleService {

    /**
     * 设置用户角色
     *
     * @param userId 用户ID
     * @param roles  角色
     */
    void setOrgUserRoles(long userId, List<Long> roles);

    /**
     * 创建角色，如果创建的是组织类型角色，那么会影响所有组织
     *
     * @param role    {@link AppRole}
     * @param isAdmin 是否为超管
     */
    void createRoleEffectOrg(AppRole role, boolean isAdmin);

    /**
     * 更新角色，如果更新的是组织类型角色，那么会影响所有组织
     *
     * @param role    {@link AppRole}
     * @param isAdmin 是否为超管
     */
    void updateRoleEffectOrg(AppRole role, boolean isAdmin);

    /**
     * 删除角色，如果删除的是组织类型角色，那么会影响所有组织
     *
     * @param id      角色ID
     * @param isAdmin 是否为超管
     */
    void removeRoleEffectOrg(long id, boolean isAdmin);

    /**
     * 创建角色组，如果创建的是组织类型角色组，那么会影响所有组织
     *
     * @param group   {@link AppRoleGroup}
     * @param isAdmin 是否为超管
     */
    void createRoleGroupEffectOrg(AppRoleGroup group, boolean isAdmin);

    /**
     * 更新角色组，如果更新的是组织类型角色组，那么会影响所有组织
     *
     * @param group   {@link AppRoleGroup}
     * @param isAdmin 是否为超管
     */
    void updateRoleGroupEffectOrg(AppRoleGroup group, boolean isAdmin);

    /**
     * 删除角色组，如果删除的是组织类型角色组，那么会影响所有组织
     *
     * @param id      组id
     * @param isAdmin 是否为超管
     */
    void removeRoleGroupEffectOrg(long id, boolean isAdmin);
}
