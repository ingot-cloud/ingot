package com.ingot.cloud.pms.service.biz;

import com.ingot.cloud.pms.api.model.domain.SysRole;
import com.ingot.cloud.pms.api.model.domain.SysRoleGroup;
import com.ingot.cloud.pms.api.model.dto.authority.AuthorityFilterDTO;
import com.ingot.cloud.pms.api.model.vo.authority.AuthorityTreeNodeVO;
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
     * 获取组织当前可以操作的权限
     *
     * @param orgId 组织ID
     * @return {@link AuthorityTreeNodeVO}
     */
    List<AuthorityTreeNodeVO> getOrgAuthority(long orgId);

    /**
     * 获取组织角色权限
     *
     * @param roleId    角色ID
     * @param condition 条件参数
     * @return {@link AuthorityTreeNodeVO}
     */
    List<AuthorityTreeNodeVO> getOrgRoleAuthorities(long roleId,
                                                    AuthorityFilterDTO condition);

    /**
     * 组织角色绑定权限
     *
     * @param params {@link RelationDTO}
     */
    void orgRoleBindAuthorities(RelationDTO<Long, Long> params);

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

    /**
     * 角色绑定权限
     *
     * @param params {@link RelationDTO}
     */
    void roleBindAuthoritiesEffectOrg(RelationDTO<Long, Long> params);
}
