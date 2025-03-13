package com.ingot.cloud.pms.service.biz;

import com.ingot.framework.core.model.security.UserDetailsRequest;
import com.ingot.framework.core.model.security.UserDetailsResponse;

/**
 * <p>Description  : SupportUserDetailsService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/13.</p>
 * <p>Time         : 10:27 AM.</p>
 */
public interface SupportUserDetailsService {

    /**
     * 是否支持请求
     *
     * @param request {@link UserDetailsRequest}
     * @return Boolean
     */
    boolean support(UserDetailsRequest request);

    /**
     * 获取用户详情
     *
     * @param request {@link UserDetailsRequest}
     * @return {@link UserDetailsResponse}
     */
    UserDetailsResponse getUserDetails(UserDetailsRequest request);
}
