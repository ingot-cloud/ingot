package com.ingot.cloud.member.service.domain;

import java.util.List;

import com.ingot.cloud.member.api.model.domain.MemberRolePermission;
import com.ingot.framework.commons.model.common.AssignDTO;
import com.ingot.framework.commons.model.common.SetDTO;
import com.ingot.framework.data.mybatis.common.service.BaseService;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author jymot
 * @since 2025-11-29
 */
public interface MemberRolePermissionService extends BaseService<MemberRolePermission> {
    /**
     * 角色设置权限
     *
     * @param params {@link AssignDTO}
     */
    void roleSetPermissions(SetDTO<Long, Long> params);

    /**
     * 角色分配权限
     *
     * @param params {@link AssignDTO}
     */
    void roleAssignPermissions(AssignDTO<Long, Long> params);

    /**
     * 获取角色绑定的权限ID列表
     *
     * @param id 角色ID
     * @return 权限ID列表
     */
    List<Long> getRoleBindPermissionIds(long id);

    /**
     * 根据权限ID清理角色权限关系
     *
     * @param permissionId 权限ID
     */
    void clearByPermissionId(long permissionId);

    /**
     * 更具角色ID清理角色权限关系
     *
     * @param roleId 角色ID
     */
    void clearByRoleId(long roleId);

    /**
     * 根据租户ID清理角色权限关系
     *
     * @param tenantId 租户ID
     */
    void clearByTenantId(long tenantId);
}
