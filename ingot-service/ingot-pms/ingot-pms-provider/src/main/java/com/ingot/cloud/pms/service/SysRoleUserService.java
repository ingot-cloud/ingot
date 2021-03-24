package com.ingot.cloud.pms.service;

import com.ingot.cloud.pms.api.model.domain.SysRoleUser;
import com.ingot.framework.store.mybatis.service.BaseService;

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
}
