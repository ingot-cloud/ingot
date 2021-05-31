package com.ingot.cloud.pms.service;

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
}
