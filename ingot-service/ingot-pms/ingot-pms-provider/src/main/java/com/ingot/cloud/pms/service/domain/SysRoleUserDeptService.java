package com.ingot.cloud.pms.service.domain;

import com.ingot.cloud.pms.api.model.domain.SysRoleUserDept;
import com.ingot.framework.data.mybatis.service.BaseService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author jymot
 * @since 2025-03-08
 */
public interface SysRoleUserDeptService extends BaseService<SysRoleUserDept> {

    /**
     * 根据部门和角色获取角色用户部门关联ID
     * @param deptId 部门ID
     * @param roleId 角色ID
     * @return {@link SysRoleUserDept#getId()}
     */
    List<Long> getRoleUserDeptIdsByDeptAndRole(long deptId, long roleId);
}
