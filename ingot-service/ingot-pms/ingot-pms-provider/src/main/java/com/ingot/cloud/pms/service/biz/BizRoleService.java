package com.ingot.cloud.pms.service.biz;

import com.ingot.cloud.pms.api.model.domain.SysRole;
import com.ingot.cloud.pms.api.model.domain.SysRoleGroup;
import com.ingot.framework.core.model.common.RelationDTO;

import java.util.List;

/**
 * <p>Description  : BizRoleService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/21.</p>
 * <p>Time         : 8:56 AM.</p>
 */
public interface BizRoleService {

    /**
     * 组织角色绑定用户
     *
     * @param params {@link RelationDTO}
     */
    void orgRoleBindUsers(RelationDTO<Long, Long> params);

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
