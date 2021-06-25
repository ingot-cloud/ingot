package com.ingot.cloud.pms.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysMenu;
import com.ingot.cloud.pms.api.model.domain.SysRoleMenu;
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
public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenu> {

    /**
     * 创建菜单角色关系，如果已存在则忽略
     *
     * @param roleId 角色ID
     * @param menuId 菜单ID
     */
    void insertIgnore(@Param("roleId") long roleId, @Param("menuId") long menuId);

    /**
     * 获取角色绑定的菜单
     *
     * @param page   分页信息
     * @param roleId 角色ID
     * @return 分页信息
     */
    IPage<SysMenu> getRoleBindMenus(Page<?> page, @Param("roleId") long roleId);
}
