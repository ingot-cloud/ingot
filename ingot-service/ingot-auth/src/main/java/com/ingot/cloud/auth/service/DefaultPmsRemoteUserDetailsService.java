package com.ingot.cloud.auth.service;

import com.ingot.cloud.pms.api.rpc.RemotePmsUserDetailsService;
import com.ingot.framework.commons.model.security.UserDetailsRequest;
import com.ingot.framework.commons.model.security.UserDetailsResponse;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.security.core.userdetails.RemoteUserDetailsService;
import lombok.RequiredArgsConstructor;

/**
 * <p>Description  : DefaultRemoteUserDetailsService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/11/30.</p>
 * <p>Time         : 4:05 PM.</p>
 */
@RequiredArgsConstructor
public class DefaultPmsRemoteUserDetailsService implements RemoteUserDetailsService {
    private final RemotePmsUserDetailsService pmsApi;

    @Override
    public boolean supports(UserDetailsRequest params) {
        return params.getUserType() == UserTypeEnum.ADMIN;
    }

    @Override
    public R<UserDetailsResponse> fetchUserDetails(UserDetailsRequest params) {
        return pmsApi.getUserAuthDetails(params);
    }
}
