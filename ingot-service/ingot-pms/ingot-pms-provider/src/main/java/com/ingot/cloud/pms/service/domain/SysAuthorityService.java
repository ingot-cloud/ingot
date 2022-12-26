package com.ingot.cloud.pms.service.domain;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.SysAuthority;
import com.ingot.cloud.pms.api.model.domain.SysRole;
import com.ingot.cloud.pms.api.model.vo.authority.AuthorityTreeNodeVO;
import com.ingot.framework.store.mybatis.service.BaseService;

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
     * 权限tree
     *
     * @return {@link AuthorityTreeNodeVO}
     */
    List<AuthorityTreeNodeVO> treeList();

    /**
     * 创建权限
     *
     * @param params 参数
     */
    void createAuthority(SysAuthority params);

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
