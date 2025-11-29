package com.ingot.cloud.pms.service.biz;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.MetaPermission;
import com.ingot.cloud.pms.api.model.domain.MetaRole;
import com.ingot.cloud.pms.api.model.vo.permission.PermissionTreeNodeVO;
import com.ingot.cloud.pms.api.model.vo.role.RoleTreeNodeVO;
import com.ingot.framework.commons.model.common.AssignDTO;
import com.ingot.framework.commons.model.common.SetDTO;
import com.ingot.framework.commons.model.support.Option;

/**
 * <p>Description  : BizMetaRoleService.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/12.</p>
 * <p>Time         : 14:48.</p>
 */
public interface BizMetaRoleService {

    /**
     * 角色下拉列表
     *
     * @param condition {@link MetaRole}
     * @return {@link Option}
     */
    List<Option<Long>> options(MetaRole condition);

    /**
     * 角色条件查询
     *
     * @param condition {@link MetaRole}
     * @return {@link RoleTreeNodeVO}
     */
    List<RoleTreeNodeVO> conditionTree(MetaRole condition);

    /**
     * 获取角色权限
     *
     * @param roleId 角色ID
     * @return {@link MetaPermission}
     */
    List<MetaPermission> getRolePermissions(long roleId);

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
     * @param params {@link MetaRole}
     */
    void create(MetaRole params);

    /**
     * 更新角色
     *
     * @param params {@link MetaRole}
     */
    void update(MetaRole params);

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
