package com.ingot.cloud.pms.service.biz;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.MetaMenu;
import com.ingot.cloud.pms.api.model.dto.menu.MenuFilterDTO;
import com.ingot.cloud.pms.api.model.types.AuthorityType;
import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNodeVO;

/**
 * <p>Description  : BizMetaMenuService.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/13.</p>
 * <p>Time         : 10:43.</p>
 */
public interface BizMetaMenuService {

    /**
     * 根据权限获取菜单
     *
     * @param authorities 权限列表
     * @return {@link MenuTreeNodeVO} list
     */
    List<MenuTreeNodeVO> getMenuByAuthorities(List<? extends AuthorityType> authorities);

    /**
     * 菜单tree
     *
     * @return {@link MenuTreeNodeVO} 节点
     */
    List<MenuTreeNodeVO> treeList(MenuFilterDTO filter);

    /**
     * 创建菜单
     *
     * @param menu {@link MetaMenu}
     */
    void create(MetaMenu menu);

    /**
     * 修改菜单
     *
     * @param menu {@link MetaMenu}
     */
    void update(MetaMenu menu);

    /**
     * 删除菜单
     *
     * @param id 菜单ID
     */
    void delete(long id);
}
