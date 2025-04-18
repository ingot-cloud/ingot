package com.ingot.cloud.pms.service.domain;

import com.ingot.cloud.pms.api.model.domain.SysAuthority;
import com.ingot.cloud.pms.api.model.domain.SysRole;
import com.ingot.cloud.pms.api.model.dto.authority.AuthorityFilterDTO;
import com.ingot.cloud.pms.api.model.vo.authority.AuthorityTreeNodeVO;
import com.ingot.framework.data.mybatis.common.service.BaseService;

import java.util.List;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
public interface SysAuthorityService extends BaseService<SysAuthority> {

    /**
     * 根据角色获取权限列表
     *
     * @param roles 角色列表
     * @return {@link SysAuthority}
     */
    List<SysAuthority> getAuthorityByRoles(List<SysRole> roles);

    /**
     * 根据角色获取权限，并且获取所以子权限
     * @param roles 角色列表
     * @return {@link SysAuthority} List
     */
    List<SysAuthority> getAuthorityAndChildrenByRoles(List<SysRole> roles);

    /**
     * 权限tree
     *
     * @return {@link AuthorityTreeNodeVO}
     */
    List<AuthorityTreeNodeVO> treeList();

    /**
     * 条件树
     *
     * @param filter 条件
     * @return {@link AuthorityFilterDTO}
     */
    List<AuthorityTreeNodeVO> treeList(AuthorityFilterDTO filter);

    /**
     * 创建权限
     *
     * @param params 参数
     * @param fillParentCode 是否填充父级code
     */
    void createAuthority(SysAuthority params, boolean fillParentCode);

    /**
     * 更新权限
     *
     * @param params 更新参数
     */
    void updateAuthority(SysAuthority params);

    /**
     * 删除权限
     *
     * @param id 权限ID
     */
    void removeAuthorityById(long id);
}
