package com.ingot.cloud.pms.service.domain;

import com.ingot.cloud.pms.api.model.domain.SysAuthority;
import com.ingot.cloud.pms.api.model.domain.SysRoleAuthority;
import com.ingot.cloud.pms.api.model.vo.authority.AuthorityTreeNodeVO;
import com.ingot.framework.core.model.common.RelationDTO;
import com.ingot.framework.data.mybatis.service.BaseService;

import java.util.List;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
public interface SysRoleAuthorityService extends BaseService<SysRoleAuthority> {
    /**
     * 权限绑定角色
     *
     * @param params 关联参数
     */
    void authorityBindRoles(RelationDTO<Long, Long> params);

    /**
     * 角色绑定权限
     *
     * @param params 关联参数
     */
    void roleBindAuthorities(RelationDTO<Long, Long> params);

    /**
     * 清空指定角色列表相应绑定数据
     *
     * @param roleIds 角色ID
     */
    void clearRole(List<Long> roleIds);

    /**
     * 清空指定角色的指定权限
     *
     * @param roleIds      角色ID
     * @param authorityIds 权限ID
     */
    void clearRoleWithAuthorities(List<Long> roleIds, List<Long> authorityIds);

    /**
     * 获取角色权限列表
     *
     * @param roleId 角色ID
     * @return {@link SysAuthority} List
     */
    List<SysAuthority> getAuthoritiesByRole(long roleId);

    /**
     * 获取角色权限信息
     *
     * @param roleId    角色ID
     * @param isBind    是否绑定
     * @param condition 条件
     * @return 分页信息
     */
    List<AuthorityTreeNodeVO> getRoleAuthorities(long roleId,
                                                 SysAuthority condition);
}
