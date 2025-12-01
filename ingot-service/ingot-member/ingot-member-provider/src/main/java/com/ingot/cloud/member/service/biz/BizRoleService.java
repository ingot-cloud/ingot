package com.ingot.cloud.member.service.biz;

import java.util.List;

import com.ingot.cloud.member.api.model.domain.MemberPermission;
import com.ingot.cloud.member.api.model.domain.MemberRole;
import com.ingot.cloud.member.api.model.vo.permission.MemberPermissionTreeNodeVO;
import com.ingot.cloud.member.api.model.vo.role.MemberRoleTreeNodeVO;
import com.ingot.framework.commons.model.common.AssignDTO;
import com.ingot.framework.commons.model.common.SetDTO;
import com.ingot.framework.commons.model.support.Option;

/**
 * <p>Description  : BizRoleService.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/1.</p>
 * <p>Time         : 10:55.</p>
 */
public interface BizRoleService {
    /**
     * 角色下拉列表
     *
     * @param condition {@link MemberRole}
     * @return {@link Option}
     */
    List<Option<Long>> options(MemberRole condition);

    /**
     * 角色条件查询
     *
     * @param condition {@link MemberRole}
     * @return {@link MemberRoleTreeNodeVO}
     */
    List<MemberRoleTreeNodeVO> conditionTree(MemberRole condition);

    /**
     * 获取角色权限
     *
     * @param roleId 角色ID
     * @return {@link MemberPermission}
     */
    List<MemberPermission> getRolePermissions(long roleId);

    /**
     * 获取角色权限
     *
     * @param roleId 角色ID
     * @return {@link MemberPermissionTreeNodeVO}
     */
    List<MemberPermissionTreeNodeVO> getRolePermissionsTree(long roleId);

    /**
     * 创建角色
     *
     * @param params {@link MemberRole}
     */
    void create(MemberRole params);

    /**
     * 更新角色
     *
     * @param params {@link MemberRole}
     */
    void update(MemberRole params);

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

    /**
     * 角色分配用户
     *
     * @param params {@link AssignDTO}
     */
    void assignUsers(AssignDTO<Long, Long> params);
}
