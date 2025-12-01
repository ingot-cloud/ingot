package com.ingot.cloud.member.service.biz;

import java.util.List;

import com.ingot.cloud.member.api.model.domain.MemberPermission;
import com.ingot.cloud.member.api.model.vo.permission.MemberPermissionTreeNodeVO;

/**
 * <p>Description  : BizPermissionService.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/1.</p>
 * <p>Time         : 11:14.</p>
 */
public interface BizPermissionService {

    /**
     * 获取权限树
     *
     * @param filter {@link MemberPermission} 过滤条件
     * @return {@link MemberPermissionTreeNodeVO}
     */
    List<MemberPermissionTreeNodeVO> treeList(MemberPermission filter);

    /**
     * 创建非菜单权限
     *
     * @param permission {@link MemberPermission}
     */
    void createNonMenuPermission(MemberPermission permission);

    /**
     * 修改非菜单权限
     *
     * @param permission {@link MemberPermission}
     */
    void updateNonMenuPermission(MemberPermission permission);

    /**
     * 删除非菜单权限，会删除一些绑定数据
     *
     * @param id 权限ID
     */
    void deleteNonMenuPermission(long id);
}
