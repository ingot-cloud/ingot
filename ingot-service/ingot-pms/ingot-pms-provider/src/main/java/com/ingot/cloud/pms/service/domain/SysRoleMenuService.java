package com.ingot.cloud.pms.service.domain;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.SysMenu;
import com.ingot.cloud.pms.api.model.domain.SysRoleMenu;
import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNodeVO;
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
    void menuBindRoles(RelationDto<Integer, Integer> params);

    /**
     * 角色绑定菜单
     *
     * @param params 关联参数
     */
    void roleBindMenus(RelationDto<Integer, Integer> params);

    /**
     * 获取角色菜单
     *
     * @param roleId    角色ID
     * @param isBind    是否绑定
     * @param condition 条件
     * @return 分页信息
     */
    List<MenuTreeNodeVO> getRoleMenus(int roleId,
                                      boolean isBind,
                                      SysMenu condition);
}
