package com.ingot.cloud.pms.service;

import com.ingot.cloud.pms.api.model.domain.SysMenu;
import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNode;
import com.ingot.framework.store.mybatis.service.BaseService;

import java.util.List;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
public interface SysMenuService extends BaseService<SysMenu> {
    /**
     * 菜单tree
     *
     * @return {@link MenuTreeNode} 节点
     */
    List<MenuTreeNode> tree();

    /**
     * 创建菜单
     *
     * @param params 参数
     */
    void createMenu(SysMenu params);

    /**
     * 更新菜单
     *
     * @param params 参数
     */
    void updateMenu(SysMenu params);

    /**
     * 根据id删除菜单
     *
     * @param id id
     */
    void removeMenuById(long id);
}
