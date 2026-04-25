package com.ingot.cloud.pms.service.biz;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.PlatformPermission;
import com.ingot.cloud.pms.api.model.vo.permission.PermissionTreeNodeVO;

/**
 * <p>Description  : BizPlatformAuthorityService.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/12.</p>
 * <p>Time         : 14:52.</p>
 */
public interface BizPlatformPermissionService {

    /**
     * 获取权限树
     *
     * @param filter {@link PlatformPermission} 过滤条件
     * @return {@link PermissionTreeNodeVO}
     */
    List<PermissionTreeNodeVO> treeList(PlatformPermission filter);

    /**
     * 创建非菜单权限
     *
     * @param permission {@link PlatformPermission}
     */
    void createNonMenuPermission(PlatformPermission permission);

    /**
     * 修改非菜单权限
     *
     * @param permission {@link PlatformPermission}
     */
    void updateNonMenuPermission(PlatformPermission permission);

    /**
     * 删除非菜单权限，会删除一些绑定数据
     *
     * @param id 权限ID
     */
    void deleteNonMenuPermission(long id);
}
