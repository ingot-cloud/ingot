package com.ingot.cloud.pms.service.biz;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.PlatformMenu;
import com.ingot.cloud.pms.api.model.types.PermissionType;
import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNodeVO;

/**
 * <p>Description  : BizPlatformMenuService.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/13.</p>
 * <p>Time         : 10:43.</p>
 */
public interface BizPlatformMenuService {

    /**
     * 根据权限获取菜单
     *
     * @param permissions 权限列表
     * @return {@link MenuTreeNodeVO} list
     */
    List<MenuTreeNodeVO> getMenuByPermissions(List<? extends PermissionType> permissions);

    /**
     * 菜单tree
     *
     * @return {@link MenuTreeNodeVO} 节点
     */
    List<MenuTreeNodeVO> treeList(PlatformMenu filter);

    /**
     * 创建菜单
     *
     * @param menu {@link PlatformMenu}
     */
    void create(PlatformMenu menu);

    /**
     * 修改菜单
     *
     * @param menu {@link PlatformMenu}
     */
    void update(PlatformMenu menu);

    /**
     * 删除菜单
     *
     * @param id 菜单ID
     */
    void delete(long id);
}
