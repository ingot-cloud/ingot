package com.ingot.cloud.auth.service.biz.impl;

import com.ingot.framework.commons.model.security.UserDetailsRequest;
import com.ingot.framework.commons.model.security.UserDetailsResponse;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.security.account.domain.port.outbound.AccountLockSignalPort;
import com.ingot.framework.security.core.userdetails.RemoteUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.LockedException;
import org.springframework.util.StringUtils;

/**
 * <p>RemoteUserDetails 装饰器：按登录名命中锁定信号时拒绝认证，跳过 Feign。</p>
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

    /**
     * 加载远程登录身份。已锁定账号直接进入框架认证失败处理。
     *
     * @param params 登录身份查询
     * @return 远程身份结果
     * @throws LockedException 登录名命中账号锁定信号
     */
    @Override
    public R<UserDetailsResponse> fetchUserDetails(UserDetailsRequest params) {
        if (params != null
                && params.getUserType() != null
                && StringUtils.hasText(params.getUsername())
                && accountLockSignalPort.isLockedByUsername(params.getUserType(), params.getUsername())) {
            log.info("[AccountLockSignal] Auth cache hit username={}, skip Feign", params.getUsername());
            throw new LockedException("Account is locked");
        }
        return delegate.fetchUserDetails(params);
    }
}
