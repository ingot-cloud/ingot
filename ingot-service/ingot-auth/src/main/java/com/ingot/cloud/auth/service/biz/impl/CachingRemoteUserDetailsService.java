package com.ingot.cloud.auth.service.biz.impl;

import com.ingot.framework.commons.model.security.UserDetailsRequest;
import com.ingot.framework.commons.model.security.UserDetailsResponse;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.security.account.domain.port.outbound.AccountLockSignalPort;
import com.ingot.framework.security.core.userdetails.RemoteUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * <p>RemoteUserDetails 装饰器：name key 命中锁定信号时直接返回 locked，跳过 Feign。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class CachingRemoteUserDetailsService implements RemoteUserDetailsService {

    private final RemoteUserDetailsService delegate;
    private final AccountLockSignalPort accountLockSignalPort;

    @Override
    public boolean supports(UserDetailsRequest params) {
        return delegate.supports(params);
    }

    @Override
    public R<UserDetailsResponse> fetchUserDetails(UserDetailsRequest params) {
        if (params != null
                && params.getUserType() != null
                && StringUtils.hasText(params.getUsername())
                && accountLockSignalPort.isLockedByUsername(params.getUserType(), params.getUsername())) {
            log.info("[AccountLockSignal] Auth cache hit username={}, skip Feign", params.getUsername());
            UserDetailsResponse response = new UserDetailsResponse();
            response.setUsername(params.getUsername());
            response.setUserType(params.getUserType().getValue());
            response.setLocked(true);
            response.setEnabled(true);
            return R.ok(response);
        }
        return delegate.fetchUserDetails(params);
    }
}
