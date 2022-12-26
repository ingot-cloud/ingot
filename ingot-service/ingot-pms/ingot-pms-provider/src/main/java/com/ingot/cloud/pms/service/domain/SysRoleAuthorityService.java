package com.ingot.cloud.pms.service.domain;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.SysAuthority;
import com.ingot.cloud.pms.api.model.domain.SysRoleAuthority;
import com.ingot.cloud.pms.api.model.vo.authority.AuthorityTreeNodeVO;
import com.ingot.framework.core.model.dto.common.RelationDTO;
import com.ingot.framework.store.mybatis.service.BaseService;

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
     * 获取角色权限列表
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
