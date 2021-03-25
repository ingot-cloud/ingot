package com.ingot.cloud.pms.service;

import com.ingot.cloud.pms.api.model.domain.SysRoleUser;
import com.ingot.framework.store.mybatis.service.BaseService;

import java.util.List;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
public interface SysRoleUserService extends BaseService<SysRoleUser> {

    /**
     * 根据用户ID删除相关关联角色
     *
     * @param userId 用户ID
     * @return 操作是否成功
     */
    boolean removeByUserId(long userId);

    /**
     * 更新用户角色，将用户角色更新为指定角色
     * @param userId 用户ID
     * @param roles 待设置的角色
     */
    boolean updateUserRole(long userId, List<Long> roles);
}
