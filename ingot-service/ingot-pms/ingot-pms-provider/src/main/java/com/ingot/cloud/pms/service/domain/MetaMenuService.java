package com.ingot.cloud.pms.service.domain;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.MetaMenu;
import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNodeVO;
import com.ingot.framework.data.mybatis.common.service.BaseService;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author jymot
 * @since 2025-11-12
 */
public interface MetaMenuService extends BaseService<MetaMenu> {

    /**
     * 菜单列表
     *
     * @return {@link List<MenuTreeNodeVO>}
     */
    List<MenuTreeNodeVO> nodeList();

    /**
     * 创建菜单
     *
     * @param menu {@link MetaMenu}
     */
    void create(MetaMenu menu);

    /**
     * 更新菜单
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
