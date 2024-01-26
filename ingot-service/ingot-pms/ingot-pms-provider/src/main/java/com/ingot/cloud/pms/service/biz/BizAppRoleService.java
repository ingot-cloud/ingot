package com.ingot.cloud.pms.service.biz;

import com.ingot.cloud.pms.api.model.domain.SysRole;
import com.ingot.cloud.pms.api.model.domain.SysRoleGroup;

/**
 * <p>Description  : BizAppRoleService.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2024/1/24.</p>
 * <p>Time         : 09:30.</p>
 */
public interface BizAppRoleService {
    /**
     * 创建角色，如果创建的是组织类型角色，那么会影响所有组织
     *
     * @param role    {@link SysRole}
     * @param isAdmin 是否为超管
     */
    void createRoleEffectOrg(SysRole role, boolean isAdmin);

    /**
     * 更新角色，如果更新的是组织类型角色，那么会影响所有组织
     *
     * @param role    {@link SysRole}
     * @param isAdmin 是否为超管
     */
    void updateRoleEffectOrg(SysRole role, boolean isAdmin);

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
     * @param group   {@link SysRoleGroup}
     * @param isAdmin 是否为超管
     */
    void createRoleGroupEffectOrg(SysRoleGroup group, boolean isAdmin);

    /**
     * 更新角色组，如果更新的是组织类型角色组，那么会影响所有组织
     *
     * @param group   {@link SysRoleGroup}
     * @param isAdmin 是否为超管
     */
    void updateRoleGroupEffectOrg(SysRoleGroup group, boolean isAdmin);

    /**
     * 删除角色组，如果删除的是组织类型角色组，那么会影响所有组织
     *
     * @param id      组id
     * @param isAdmin 是否为超管
     */
    void removeRoleGroupEffectOrg(long id, boolean isAdmin);
}
