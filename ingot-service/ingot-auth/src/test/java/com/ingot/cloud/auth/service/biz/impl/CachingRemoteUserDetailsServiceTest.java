package com.ingot.cloud.auth.service.biz.impl;

import com.ingot.framework.commons.model.security.UserDetailsRequest;
import com.ingot.framework.commons.model.security.UserDetailsResponse;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.security.account.domain.port.outbound.AccountLockSignalPort;
import com.ingot.framework.security.core.userdetails.RemoteUserDetailsService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CachingRemoteUserDetailsService} 锁定信号短路单元测试。
 *
 * @author jy
 * @since 1.0.0
 */
class CachingRemoteUserDetailsServiceTest {

    @Test
    void fetchUserDetails_nameKeyHit_skipsDelegate() {
        RemoteUserDetailsService delegate = mock(RemoteUserDetailsService.class);
        AccountLockSignalPort signalPort = mock(AccountLockSignalPort.class);
        when(delegate.supports(any())).thenReturn(true);
        when(signalPort.isLockedByUsername(UserTypeEnum.ADMIN, "admin")).thenReturn(true);

        CachingRemoteUserDetailsService service = new CachingRemoteUserDetailsService(delegate, signalPort);
        UserDetailsRequest request = new UserDetailsRequest();
        request.setUsername("admin");
        request.setUserType(UserTypeEnum.ADMIN);

        R<UserDetailsResponse> result = service.fetchUserDetails(request);

        assertTrue(Boolean.TRUE.equals(result.getData().getLocked()));
        verify(delegate, never()).fetchUserDetails(any());
    }

    @Test
    void fetchUserDetails_miss_delegates() {
        RemoteUserDetailsService delegate = mock(RemoteUserDetailsService.class);
        AccountLockSignalPort signalPort = mock(AccountLockSignalPort.class);
        UserDetailsRequest request = new UserDetailsRequest();
        request.setUsername("admin");
        request.setUserType(UserTypeEnum.ADMIN);
        UserDetailsResponse response = new UserDetailsResponse();
        response.setLocked(false);
        when(signalPort.isLockedByUsername(UserTypeEnum.ADMIN, "admin")).thenReturn(false);
        when(delegate.fetchUserDetails(request)).thenReturn(R.ok(response));

        CachingRemoteUserDetailsService service = new CachingRemoteUserDetailsService(delegate, signalPort);
        R<UserDetailsResponse> result = service.fetchUserDetails(request);

        assertEquals(response, result.getData());
        verify(delegate).fetchUserDetails(request);
    }
}
