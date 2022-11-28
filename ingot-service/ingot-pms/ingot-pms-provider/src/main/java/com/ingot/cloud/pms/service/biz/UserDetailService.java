package com.ingot.cloud.pms.service.biz;

import com.ingot.framework.security.core.userdetails.UserDetailsResponse;

/**
 * <p>Description  : UserDetailService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/12/29.</p>
 * <p>Time         : 5:23 下午.</p>
 */
public interface UserDetailService {

    /**
     * 获取用户详情信息
     *
     * @param username 用户名
     * @return {@link UserDetailsResponse}
     */
    UserDetailsResponse getUserAuthDetails(String username);
}
