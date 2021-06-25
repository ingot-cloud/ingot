package com.ingot.cloud.pms.service.domain;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysAuthority;
import com.ingot.cloud.pms.api.model.domain.SysRoleAuthority;
import com.ingot.framework.core.model.dto.common.RelationDto;
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
    void authorityBindRoles(RelationDto<Long, Long> params);

    /**
     * 角色绑定权限
     *
     * @param params 关联参数
     */
    void roleBindAuthorities(RelationDto<Long, Long> params);

    /**
     * 获取角色绑定的权限信息
     *
     * @param roleId 角色ID
     * @param page   分页参数
     * @return 分页信息
     */
    IPage<SysAuthority> getRoleBindAuthorities(long roleId, Page<?> page);
}
