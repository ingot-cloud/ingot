package com.ingot.framework.security.core.userdetails;

import com.ingot.framework.core.wrapper.IngotResponse;

/**
 * <p>Description  : RemoteUserDetailsService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/7.</p>
 * <p>Time         : 3:42 下午.</p>
 */
public interface RemoteUserDetailsService {

    /**
     * 获取用户详情响应信息
     *
     * @param params 请求参数
     * @return response
     */
    IngotResponse<UserDetailsResponse> fetchUserDetails(UserDetailsRequest params);
}
