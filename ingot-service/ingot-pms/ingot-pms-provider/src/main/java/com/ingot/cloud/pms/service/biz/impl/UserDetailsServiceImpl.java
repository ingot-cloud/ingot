package com.ingot.cloud.pms.service.biz.impl;

import com.ingot.cloud.pms.service.biz.SupportUserDetailsService;
import com.ingot.cloud.pms.service.biz.UserDetailsService;
import com.ingot.framework.core.model.security.UserDetailsRequest;
import com.ingot.framework.core.model.security.UserDetailsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>Description  : UserDetailServiceImpl.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/12/29.</p>
 * <p>Time         : 5:27 下午.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final List<SupportUserDetailsService> supportUserDetailsServices;

    @Override
    public UserDetailsResponse getUserDetails(UserDetailsRequest params) {
        for (SupportUserDetailsService service : supportUserDetailsServices) {
            if (service.support(params)) {
                return service.getUserDetails(params);
            }
        }
        return null;
    }
}
