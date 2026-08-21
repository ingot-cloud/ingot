package com.ingot.framework.security.account.adapter.port;

import com.ingot.cloud.auth.api.model.dto.InnerUserSessionRevokeDTO;
import com.ingot.cloud.auth.api.rpc.RemoteAuthSessionService;
import com.ingot.framework.commons.model.security.SessionRevokeReason;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link FeignSessionRevocationAdapter} 联动与 fail-open 单元测试。
 *
 * @author jy
 * @since 1.0.0
 */
class FeignSessionRevocationAdapterTest {

    private final RemoteAuthSessionService remoteAuthSessionService = mock(RemoteAuthSessionService.class);
    private final FeignSessionRevocationAdapter adapter =
            new FeignSessionRevocationAdapter(remoteAuthSessionService);

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void revoke_withTenantContext_passesReasonAndActor() {
        TenantContextHolder.set(1L);
        when(remoteAuthSessionService.revokeByUser(any())).thenReturn(R.ok(2));

        int revoked = adapter.revokeUserSessions(10L, SessionRevokeReason.ACCOUNT_LOCKED, 9L);

        ArgumentCaptor<InnerUserSessionRevokeDTO> captor =
                ArgumentCaptor.forClass(InnerUserSessionRevokeDTO.class);
        verify(remoteAuthSessionService).revokeByUser(captor.capture());
        InnerUserSessionRevokeDTO params = captor.getValue();
        assertEquals(1L, params.getTenantId().longValue());
        assertEquals(10L, params.getUserId().longValue());
        assertEquals(SessionRevokeReason.ACCOUNT_LOCKED, params.getReason());
        assertEquals(9L, params.getActorId().longValue());
        // clientId 留空表示覆盖该租户下全部登录入口
        assertNull(params.getClientId());
        assertEquals(2, revoked);
        assertEquals(2L, adapter.getRevokedSessions());
        assertEquals(0L, adapter.getFailures());
    }

    @Test
    void revoke_withoutTenantContext_skipsCall() {
        int revoked = adapter.revokeUserSessions(10L, SessionRevokeReason.PASSWORD_CHANGED, null);

        verify(remoteAuthSessionService, never()).revokeByUser(any());
        assertEquals(0, revoked);
        assertEquals(1L, adapter.getFailures());
    }

    @Test
    void revoke_authError_failsOpen() {
        TenantContextHolder.set(1L);
        when(remoteAuthSessionService.revokeByUser(any())).thenReturn(R.error500("boom"));

        int revoked = adapter.revokeUserSessions(10L, SessionRevokeReason.PASSWORD_CHANGED, null);

        assertEquals(0, revoked);
        assertEquals(1L, adapter.getFailures());
    }

    @Test
    void revoke_rpcException_failsOpen() {
        TenantContextHolder.set(1L);
        when(remoteAuthSessionService.revokeByUser(any())).thenThrow(new IllegalStateException("down"));

        int revoked = adapter.revokeUserSessions(10L, SessionRevokeReason.ACCOUNT_DISABLED, 9L);

        assertEquals(0, revoked);
        assertEquals(1L, adapter.getFailures());
    }

    @Test
    void revoke_nullUser_isNoOp() {
        TenantContextHolder.set(1L);

        assertEquals(0, adapter.revokeUserSessions(null, SessionRevokeReason.ADMIN_REVOKE, null));
        verify(remoteAuthSessionService, never()).revokeByUser(any());
    }
}
