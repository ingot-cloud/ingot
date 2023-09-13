package com.ingot.cloud.pms.service.biz.impl;

import com.ingot.cloud.pms.service.biz.SupportUserDetailsService;
import com.ingot.framework.security.common.constants.UserType;
import com.ingot.framework.security.core.userdetails.UserDetailsRequest;
import com.ingot.framework.security.core.userdetails.UserDetailsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>Description  : AppSupportUserDetailsService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/13.</p>
 * <p>Time         : 10:31 AM.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppSupportUserDetailsService implements SupportUserDetailsService {

    @Override
    public boolean support(UserDetailsRequest request) {
        return request.getUserType() == UserType.APP;
    }

    @Override
    public UserDetailsResponse getUserDetails(UserDetailsRequest params) {
        return null;
    }

}
