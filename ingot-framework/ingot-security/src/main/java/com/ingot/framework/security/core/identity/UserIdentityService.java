package com.ingot.framework.security.core.identity;

import com.ingot.framework.commons.model.security.UserDetailsRequest;
import com.ingot.framework.commons.model.security.UserDetailsResponse;

/**
 * <p>Description  : UserIdentityService.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/1.</p>
 * <p>Time         : 17:47.</p>
 */
public interface UserIdentityService {

    /**
     * 获取用户详情信息
     *
     * @param request {@link UserDetailsRequest}
     * @return {@link UserDetailsResponse}
     */
    UserDetailsResponse loadUser(UserDetailsRequest request);
}
