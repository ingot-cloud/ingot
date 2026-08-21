package com.ingot.framework.security.oauth2.server.authorization.session;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.ingot.framework.commons.model.security.SessionRevokeReason;
import com.ingot.framework.security.oauth2.server.authorization.OnlineToken;
import com.ingot.framework.security.oauth2.server.authorization.OnlineTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link DefaultSessionRevocationService} 撤销 Authorization / 在线会话与事件回调的联动。
 *
 * @author jy
 * @since 1.0.0
 */
class DefaultSessionRevocationServiceTest {

    private static final String SID = "session-1";
    private static final String CLIENT_ID = "web";
    private static final Long ACTOR_ID = 99L;

    private final OAuth2AuthorizationService authorizationService = mock(OAuth2AuthorizationService.class);
    private final OnlineTokenService onlineTokenService = mock(OnlineTokenService.class);
    private final List<SessionRevokedEvent> events = new ArrayList<>();
    private final DefaultSessionRevocationService service = new DefaultSessionRevocationService(
            authorizationService, onlineTokenService, List.of(events::add));

    @Test
    void revokeBySid_removesAuthorizationAndSession() {
        OAuth2Authorization authorization = authorization();
        when(onlineTokenService.getBySid(SID)).thenReturn(Optional.of(session()));
        when(authorizationService.findById(SID)).thenReturn(authorization);

        assertTrue(service.revokeBySid(SID, SessionRevokeReason.USER_LOGOUT));

        verify(authorizationService).remove(authorization);
        verify(onlineTokenService).removeBySid(SID);
    }

    @Test
    void revokeBySid_authorizationMissing_stillClearsSession() {
        when(onlineTokenService.getBySid(SID)).thenReturn(Optional.of(session()));
        when(authorizationService.findById(SID)).thenReturn(null);

        assertTrue(service.revokeBySid(SID, SessionRevokeReason.ADMIN_REVOKE));

        verify(onlineTokenService).removeBySid(SID);
    }

    @Test
    void revokeBySid_alreadyOffline_returnsFalse() {
        when(onlineTokenService.getBySid(SID)).thenReturn(Optional.empty());
        when(authorizationService.findById(SID)).thenReturn(null);

        assertFalse(service.revokeBySid(SID, SessionRevokeReason.ADMIN_REVOKE));
    }

    @Test
    void revokeBySid_blankSid_isNoOp() {
        assertFalse(service.revokeBySid("", SessionRevokeReason.ADMIN_REVOKE));

        verify(onlineTokenService, never()).removeBySid("");
    }

    @Test
    void revokeByUser_revokesEveryOnlineSession() {
        when(onlineTokenService.listSids(1L, CLIENT_ID, 9L)).thenReturn(List.of(SID, "session-2"));
        when(onlineTokenService.getBySid(SID)).thenReturn(Optional.of(session()));
        when(onlineTokenService.getBySid("session-2")).thenReturn(Optional.of(session()));

        assertEquals(2, service.revokeByUser(1L, CLIENT_ID, 9L, SessionRevokeReason.PASSWORD_CHANGED));

        verify(onlineTokenService).removeBySid(SID);
        verify(onlineTokenService).removeBySid("session-2");
    }

    @Test
    void revokeByUser_blankClientId_listsAcrossClients() {
        when(onlineTokenService.listSids(1L, 9L)).thenReturn(List.of(SID));
        when(onlineTokenService.getBySid(SID)).thenReturn(Optional.of(session()));

        assertEquals(1, service.revokeByUser(1L, null, 9L, SessionRevokeReason.ACCOUNT_LOCKED, ACTOR_ID));

        verify(onlineTokenService, never()).listSids(1L, null, 9L);
    }

    @Test
    void revokeBySid_online_notifiesListenerWithSnapshotAndActor() {
        when(onlineTokenService.getBySid(SID)).thenReturn(Optional.of(session()));
        when(authorizationService.findById(SID)).thenReturn(null);

        service.revokeBySid(SID, SessionRevokeReason.CONCURRENT_KICKOUT, ACTOR_ID);

        assertEquals(1, events.size());
        SessionRevokedEvent event = events.get(0);
        assertEquals(SID, event.sid());
        assertEquals(SessionRevokeReason.CONCURRENT_KICKOUT, event.reason());
        assertEquals(ACTOR_ID, event.actorId());
        // 快照必须是删除前读到的会话，否则事件里拿不到用户与登录环境
        assertEquals(9L, event.session().getUserId());
    }

    @Test
    void revokeBySid_offline_doesNotNotifyListener() {
        when(onlineTokenService.getBySid(SID)).thenReturn(Optional.empty());
        when(authorizationService.findById(SID)).thenReturn(null);

        service.revokeBySid(SID, SessionRevokeReason.ADMIN_REVOKE, ACTOR_ID);

        assertTrue(events.isEmpty());
    }

    @Test
    void revokeBySid_listenerFailure_doesNotBreakRevoke() {
        SessionRevocationService failing = new DefaultSessionRevocationService(
                authorizationService, onlineTokenService, List.of(event -> {
            throw new IllegalStateException("publish failed");
        }));
        when(onlineTokenService.getBySid(SID)).thenReturn(Optional.of(session()));
        when(authorizationService.findById(SID)).thenReturn(null);

        assertTrue(failing.revokeBySid(SID, SessionRevokeReason.ADMIN_REVOKE, ACTOR_ID));

        verify(onlineTokenService).removeBySid(SID);
    }

    private static OnlineToken session() {
        return OnlineToken.builder()
                .sid(SID)
                .userId(9L)
                .tenantId(1L)
                .clientId(CLIENT_ID)
                .principalName("admin")
                .build();
    }

    private OAuth2Authorization authorization() {
        return OAuth2Authorization.withRegisteredClient(
                        RegisteredClient.withId("client-1")
                                .clientId(CLIENT_ID)
                                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                                .build())
                .id(SID)
                .principalName("admin")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .build();
    }
}
