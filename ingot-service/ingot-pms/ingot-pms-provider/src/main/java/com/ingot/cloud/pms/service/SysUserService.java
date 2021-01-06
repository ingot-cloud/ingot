package com.ingot.cloud.pms.service;

import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.api.model.dto.user.UserInfoDto;
import com.ingot.framework.security.core.userdetails.IngotUser;
import com.ingot.framework.store.mybatis.service.BaseService;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
public interface SysUserService extends BaseService<SysUser> {

    /**
     * 通过 {@link IngotUser} 获取用户信息
     *
     * @param user {@link IngotUser} 当前登录用户
     * @return {@link UserInfoDto}
     */
    UserInfoDto getUserInfo(IngotUser user);
}
