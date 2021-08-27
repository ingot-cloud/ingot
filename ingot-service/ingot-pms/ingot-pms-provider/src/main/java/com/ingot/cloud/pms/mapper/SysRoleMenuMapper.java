package com.ingot.cloud.pms.mapper;

import com.ingot.cloud.pms.api.model.domain.SysMenu;
import com.ingot.cloud.pms.api.model.domain.SysRoleMenu;
import com.ingot.framework.store.mybatis.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenu> {

    /**
     * 创建菜单角色关系，如果已存在则忽略
     *
     * @param roleId 角色ID
     * @param menuId 菜单ID
     */
    void insertIgnore(@Param("roleId") long roleId, @Param("menuId") long menuId);

    /**
     * 获取角色菜单
     *
     * @param roleId    角色ID
     * @param isBind    是否绑定
     * @param condition 条件
     * @return 分页信息
     */
    List<SysMenu> getRoleMenus(@Param("roleId") long roleId,
                               @Param("isBind") boolean isBind,
                               @Param("condition") SysMenu condition);
}
