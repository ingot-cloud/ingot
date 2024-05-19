package com.ingot.cloud.pms.service.biz;

import com.ingot.cloud.pms.api.model.domain.SysMenu;

/**
 * <p>Description  : BizMenuService.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2024/5/19.</p>
 * <p>Time         : 08:15.</p>
 */
public interface BizMenuService {

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
