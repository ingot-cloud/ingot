package com.ingot.cloud.pms.service.domain;

import com.ingot.cloud.pms.api.model.domain.AppRole;
import com.ingot.framework.data.mybatis.service.BaseService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author jymot
 * @since 2023-09-12
 */
public interface AppRoleService extends BaseService<AppRole> {
    /**
     * 获取用户所有可用角色，包括用户基本角色和部门角色
     *
     * @param userId 用户ID
     * @param deptId 部门ID
     * @return 角色列表
     */
    List<AppRole> getAllRolesOfUser(long userId, long deptId);
}
