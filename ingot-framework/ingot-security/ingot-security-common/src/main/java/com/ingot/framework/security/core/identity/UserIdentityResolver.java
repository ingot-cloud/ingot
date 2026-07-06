package com.ingot.framework.security.core.identity;

import com.ingot.framework.commons.model.security.UserDetailsRequest;
import com.ingot.framework.commons.model.security.UserDetailsResponse;
import com.ingot.framework.commons.model.security.UserIdentityTypeEnum;

/**
 * <p>Description  : UserIdentityResolver.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/1.</p>
 * <p>Time         : 17:50.</p>
 */
public interface UserIdentityResolver {

    /**
     * 是否支持
     *
     * @param type 类型
     * @return true/false
     */
    boolean supports(UserIdentityTypeEnum type);

    /**
     * 获取用户详情
     *
     * @param request {@link UserDetailsRequest}
     * @return {@link UserDetailsResponse}
     */
    UserDetailsResponse load(UserDetailsRequest request);
}
