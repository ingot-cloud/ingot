package com.ingot.cloud.pms.service.domain;

import java.util.List;

import com.ingot.cloud.pms.api.model.bo.role.BizAssignRoleBO;
import com.ingot.cloud.pms.api.model.bo.role.BizRoleAssignUsersBO;
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
     * 获取用户关联角色相关信息
     *
     * @param userId 用户ID
     * @return {@link TenantRoleUserPrivate}
     */
    List<TenantRoleUserPrivate> getUserRoles(long userId);

    /**
     * 获取角色关联用户ID列表
     *
     * @param roleId 角色ID
     * @return {@link TenantRoleUserPrivate}
     */
    List<TenantRoleUserPrivate> listRoleUsers(long roleId);

    /**
     * 角色绑定用户
     *
     * @param params {@link BizRoleAssignUsersBO}
     */
    void roleBindUsers(BizRoleAssignUsersBO params);

    /**
     * 用户绑定角色
     *
     * @param userId 用户ID
     * @param roles  角色列表
     */
    void setRoles(long userId, List<BizAssignRoleBO> roles);

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
     * 根据部门ID清空角色用户关系
     *
     * @param deptId 部门ID
     */
    void clearByDeptId(long deptId);

    /**
     * 根据角色和部门清空角色用户关系
     *
     * @param roleId 角色ID
     * @param deptId 部门ID
     */
    void clearByRoleAndDept(long roleId, long deptId);

    /**
     * 根据租户ID清空角色用户关系
     * @param tenantId 租户ID
     */
    void clearByTenantId(long tenantId);

}
