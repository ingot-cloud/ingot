package com.ingot.cloud.pms.service.domain;

import com.ingot.cloud.pms.api.model.domain.SysAuthority;
import com.ingot.cloud.pms.api.model.domain.SysMenu;
import com.ingot.cloud.pms.api.model.dto.menu.MenuFilterDTO;
import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNodeVO;
import com.ingot.framework.data.mybatis.service.BaseService;

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
     * 根据权限获取菜单
     *
     * @param authorities 角色列表
     * @return {@link MenuTreeNodeVO} list
     */
    List<MenuTreeNodeVO> getMenuByAuthorities(List<SysAuthority> authorities);

    /**
     * 获取所有菜单，非树形结构
     * @return {@link MenuTreeNodeVO} List
     */
    List<MenuTreeNodeVO> nodeList();

    /**
     * 菜单tree
     *
     * @return {@link MenuTreeNodeVO} 节点
     */
    List<MenuTreeNodeVO> treeList(MenuFilterDTO filter);

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
