package com.ingot.cloud.pms.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysAuthority;
import com.ingot.cloud.pms.api.model.domain.SysRoleAuthority;
import com.ingot.framework.store.mybatis.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
public interface SysRoleAuthorityMapper extends BaseMapper<SysRoleAuthority> {

    /**
     * 创建用户角色关系，如果已存在则忽略
     *
     * @param roleId      角色ID
     * @param authorityId 权限ID
     */
    void insertIgnore(@Param("roleId") long roleId, @Param("authorityId") long authorityId);

    /**
     * 获取角色权限信息
     *
     * @param page   分页信息
     * @param roleId 角色ID
     * @param isBind 是否绑定
     * @return 分页信息
     */
    IPage<SysAuthority> getRoleAuthorities(Page<?> page,
                                           @Param("roleId") long roleId,
                                           @Param("isBind") boolean isBind);
}
