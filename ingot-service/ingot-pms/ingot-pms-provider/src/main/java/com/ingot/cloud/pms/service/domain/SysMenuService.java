package com.ingot.cloud.pms.service.domain;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.SysMenu;
import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNodeVO;
import com.ingot.framework.store.mybatis.service.BaseService;

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
     * @return {@link MenuTreeNodeVO} 节点
     */
    List<MenuTreeNodeVO> tree();

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
    void removeMenuById(int id);
}
