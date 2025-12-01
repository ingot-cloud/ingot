package com.ingot.cloud.member.service.domain;

import java.util.List;

import com.ingot.cloud.member.api.model.domain.MemberRoleUser;
import com.ingot.framework.commons.model.common.AssignDTO;
import com.ingot.framework.data.mybatis.common.service.BaseService;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author jymot
 * @since 2025-11-29
 */
public interface MemberRoleUserService extends BaseService<MemberRoleUser> {

    /**
     * 获取用户关联角色相关信息
     *
     * @param userId 用户ID
     * @return {@link MemberRoleUser}
     */
    List<MemberRoleUser> getUserRoles(long userId);

    /**
     * 获取角色关联用户ID列表
     *
     * @param roleId 角色ID
     * @return {@link MemberRoleUser}
     */
    List<MemberRoleUser> listRoleUsers(long roleId);

    /**
     * 角色绑定用户
     *
     * @param params {@link AssignDTO}
     */
    void roleBindUsers(AssignDTO<Long, Long> params);

    /**
     * 用户绑定角色
     *
     * @param userId 用户ID
     * @param roles  角色列表
     */
    void setRoles(long userId, List<Long> roles);

    /**
     * 根据角色ID清空角色用户关系
     *
     * @param id 角色ID
     */
    void clearByRoleId(long id);

    /**
     * 根据用户ID清空角色用户关系
     *
     * @param userId 用户ID
     */
    void clearByUserId(long userId);

    /**
     * 根据租户ID清空角色用户关系
     *
     * @param tenantId 租户ID
     */
    void clearByTenantId(long tenantId);
}
