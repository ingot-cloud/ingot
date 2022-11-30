package com.ingot.cloud.auth.service;

import com.ingot.cloud.pms.api.rpc.PmsUserAuthFeignApi;
import com.ingot.framework.core.wrapper.R;
import com.ingot.framework.security.core.userdetails.RemoteUserDetailsService;
import com.ingot.framework.security.core.userdetails.UserDetailsRequest;
import com.ingot.framework.security.core.userdetails.UserDetailsResponse;
import lombok.RequiredArgsConstructor;

/**
 * <p>Description  : DefaultRemoteUserDetailsService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/11/30.</p>
 * <p>Time         : 4:05 PM.</p>
 */
@RequiredArgsConstructor
public class DefaultRemoteUserDetailsService implements RemoteUserDetailsService {
    private final PmsUserAuthFeignApi pmsApi;

    @Override
    public R<UserDetailsResponse> fetchUserDetails(UserDetailsRequest params) {
        return pmsApi.getUserAuthDetails(params.getUsername());
    }

    @Override
    public R<UserDetailsResponse> fetchUserDetailsSocial(UserDetailsRequest params) {
        return pmsApi.getUserAuthDetailsSocial(params.getUsername());
    }
}
