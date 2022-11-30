package com.ingot.framework.security.core.userdetails;

import com.ingot.framework.core.wrapper.R;

/**
 * <p>Description  : RemoteUserDetailsService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/7.</p>
 * <p>Time         : 3:42 下午.</p>
 */
public interface RemoteUserDetailsService {

    /**
     * 密码模式，获取用户详情
     *
     * @param params 详情请求参数
     * @return {@link UserDetailsResponse}
     */
    R<UserDetailsResponse> fetchUserDetails(UserDetailsRequest params);

    /**
     * 社交模式，获取用户详情
     *
     * @param params 详情请求参数
     * @return {@link UserDetailsResponse}
     */
    R<UserDetailsResponse> fetchUserDetailsSocial(UserDetailsRequest params);
}
