package com.ingot.cloud.pms.service.domain;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.PlatformMenuPermission;
import com.ingot.framework.data.mybatis.common.service.BaseService;

/**
 * <p>菜单可见性关联的领域服务。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public interface PlatformMenuPermissionService extends BaseService<PlatformMenuPermission> {

    /**
     * 读取菜单关联的权限 ID，顺序与保存顺序一致。
     *
     * @param menuId 菜单 ID
     * @return 权限 ID 列表，无关联时为空列表
     */
    List<Long> listPermissionIds(long menuId);

    /**
     * 按菜单分组读取全部可见性关联，供菜单树装配。
     *
     * @return 菜单 ID → 权限 ID 列表
     */
    java.util.Map<Long, List<Long>> mapPermissionIds();

    /**
     * 整体替换菜单的可见性关联。
     *
     * @param menuId        菜单 ID
     * @param permissionIds 具体权限 ID，可空表示清空
     */
    void replace(long menuId, List<Long> permissionIds);

    /**
     * 删除指定菜单的全部可见性关联。
     *
     * @param menuId 菜单 ID
     */
    void clearByMenuId(long menuId);

    /**
     * 删除引用指定权限的全部可见性关联。
     *
     * @param permissionId 权限 ID
     */
    void clearByPermissionId(long permissionId);
}
