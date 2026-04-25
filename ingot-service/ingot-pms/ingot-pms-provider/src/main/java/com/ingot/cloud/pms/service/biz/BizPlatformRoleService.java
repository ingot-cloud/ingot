package com.ingot.cloud.pms.service.biz;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.PlatformPermission;
import com.ingot.cloud.pms.api.model.domain.PlatformRole;
import com.ingot.cloud.pms.api.model.vo.permission.PermissionTreeNodeVO;
import com.ingot.cloud.pms.api.model.vo.role.RoleTreeNodeVO;
import com.ingot.framework.commons.model.common.AssignDTO;
import com.ingot.framework.commons.model.common.SetDTO;
import com.ingot.framework.commons.model.support.Option;

/**
 * <p>Description  : BizPlatformRoleService.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/12.</p>
 * <p>Time         : 14:48.</p>
 */
public interface BizPlatformRoleService {

    /**
     * 角色下拉列表
     *
     * @param condition {@link PlatformRole}
     * @return {@link Option}
     */
    List<Option<Long>> options(PlatformRole condition);

    /**
     * 角色条件查询
     *
     * @param condition {@link PlatformRole}
     * @return {@link RoleTreeNodeVO}
     */
    List<RoleTreeNodeVO> conditionTree(PlatformRole condition);

    /**
     * 获取角色权限
     *
     * @param roleId 角色ID
     * @return {@link PlatformPermission}
     */
    List<PlatformPermission> getRolePermissions(long roleId);

    /**
     * 获取角色权限
     *
     * @param roleId 角色ID
     * @return {@link PermissionTreeNodeVO}
     */
    List<PermissionTreeNodeVO> getRolePermissionsTree(long roleId);

    /**
     * 创建角色
     *
     * @param params {@link PlatformRole}
     */
    void create(PlatformRole params);

    /**
     * 更新角色
     *
     * @param params {@link PlatformRole}
     */
    void update(PlatformRole params);

    /**
     * 删除角色
     *
     * @param id 角色ID
     */
    void delete(long id);

    /**
     * 绑定权限
     *
     * @param params {@link AssignDTO}
     */
    void setPermissions(SetDTO<Long, Long> params);
}
