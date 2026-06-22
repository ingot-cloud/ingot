package com.ingot.cloud.pms.service.domain;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.PlatformMenu;
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
public interface PlatformMenuService extends BaseService<PlatformMenu> {

    /**
     * 菜单列表
     *
     * @return {@link List<MenuTreeNodeVO>}
     */
    List<MenuTreeNodeVO> nodeList();

    /**
     * 创建菜单
     *
     * @param menu {@link PlatformMenu}
     */
    void create(PlatformMenu menu);

    /**
     * 更新菜单
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

    /**
     * 删除指定应用下的全部菜单（用于应用强制删除级联）。
     *
     * @param appId 应用ID
     */
    void deleteByAppId(long appId);
}
