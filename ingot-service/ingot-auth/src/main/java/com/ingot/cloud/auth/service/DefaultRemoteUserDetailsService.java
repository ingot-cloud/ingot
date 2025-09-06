package com.ingot.cloud.auth.service;

import com.ingot.cloud.pms.api.rpc.PmsUserDetailsService;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.security.core.userdetails.RemoteUserDetailsService;
import com.ingot.framework.commons.model.security.UserDetailsRequest;
import com.ingot.framework.commons.model.security.UserDetailsResponse;
import lombok.RequiredArgsConstructor;

/**
 * <p>Description  : DefaultRemoteUserDetailsService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/11/30.</p>
 * <p>Time         : 4:05 PM.</p>
 */
@RequiredArgsConstructor
public class DefaultRemoteUserDetailsService implements RemoteUserDetailsService {
    private final PmsUserDetailsService pmsApi;

    @Override
    public R<UserDetailsResponse> fetchUserDetails(UserDetailsRequest params) {
        return pmsApi.getUserAuthDetails(params);
    }
}
