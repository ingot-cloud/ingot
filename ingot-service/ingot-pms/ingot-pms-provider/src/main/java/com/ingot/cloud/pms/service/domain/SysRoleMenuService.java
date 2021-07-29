package com.ingot.cloud.pms.service.domain;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysMenu;
import com.ingot.cloud.pms.api.model.domain.SysRoleMenu;
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
public interface SysRoleMenuService extends BaseService<SysRoleMenu> {
    /**
     * 菜单绑定角色
     *
     * @param params 关联参数
     */
    void menuBindRoles(RelationDto<Long, Long> params);

    /**
     * 角色绑定菜单
     *
     * @param params 关联参数
     */
    void roleBindMenus(RelationDto<Long, Long> params);

    /**
     * 获取角色菜单
     *
     * @param roleId 角色ID
     * @param page   分页信息
     * @param isBind 是否绑定
     * @return 分页信息
     */
    IPage<SysMenu> getRoleMenus(long roleId, Page<?> page, boolean isBind);
}
