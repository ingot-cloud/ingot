package com.ingot.cloud.auth.service.biz.impl;

import com.ingot.cloud.member.api.rpc.RemoteMemberUserDetailsService;
import com.ingot.framework.commons.model.security.UserDetailsRequest;
import com.ingot.framework.commons.model.security.UserDetailsResponse;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.security.core.userdetails.RemoteUserDetailsService;
import lombok.RequiredArgsConstructor;

/**
 * <p>Description  : DefaultMemberRemoteUserDetailsService.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/1.</p>
 * <p>Time         : 15:23.</p>
 */
@RequiredArgsConstructor
public class DefaultMemberRemoteUserDetailsService implements RemoteUserDetailsService {
    private final RemoteMemberUserDetailsService memberApi;

    @Override
    public boolean supports(UserDetailsRequest params) {
        return params.getUserType() == UserTypeEnum.APP;
    }

    @Override
    public R<UserDetailsResponse> fetchUserDetails(UserDetailsRequest params) {
        return memberApi.getUserAuthDetails(params);
    }
}
