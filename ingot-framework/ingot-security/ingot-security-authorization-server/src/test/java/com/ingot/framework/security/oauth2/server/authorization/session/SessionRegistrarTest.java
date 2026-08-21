package com.ingot.framework.security.oauth2.server.authorization.session;

import java.time.Instant;
import java.util.List;

import com.ingot.framework.commons.model.security.TokenAuthTypeEnum;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.core.userdetails.InUser;
import com.ingot.framework.security.oauth2.server.authorization.OnlineSessionRegistration;
import com.ingot.framework.security.oauth2.server.authorization.OnlineTokenService;
import com.ingot.framework.security.oauth2.server.authorization.session.concurrency.SessionConcurrencyEnforcer;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link SessionRegistrar} 与并发约束执行面的协作行为。
 *
 * @author jy
 * @since 1.0.0
 */
class SessionRegistrarTest {

    private static final String SID = "session-new";
    private static final long USER_ID = 9L;
    private static final long TENANT_ID = 1L;
    private static final String CLIENT_ID = "web";

    private final OnlineTokenService onlineTokenService = mock(OnlineTokenService.class);
    private final SessionConcurrencyEnforcer concurrencyEnforcer = mock(SessionConcurrencyEnforcer.class);
    private final SessionRegistrar registrar = new SessionRegistrar(onlineTokenService, concurrencyEnforcer);

    @Test
    void newLogin_enforcesConcurrencyBeforeSave() {
        when(onlineTokenService.isOnlineSid(SID)).thenReturn(false);

        registrar.register(user(), registration());

        verify(concurrencyEnforcer).enforce(any(InUser.class), anyString());
        verify(onlineTokenService).save(any(InUser.class), any(OnlineSessionRegistration.class));
    }

    @Test
    void refresh_sameSid_skipsConcurrencyCheck() {
        when(onlineTokenService.isOnlineSid(SID)).thenReturn(true);

        registrar.register(user(), registration());

        verify(concurrencyEnforcer, never()).enforce(any(InUser.class), anyString());
        verify(onlineTokenService).save(any(InUser.class), any(OnlineSessionRegistration.class));
    }

    @Test
    void rejectedLogin_doesNotPersistSession() {
        when(onlineTokenService.isOnlineSid(SID)).thenReturn(false);
        doThrow(new OAuth2AuthenticationException(new OAuth2Error("concurrent_session_limit")))
                .when(concurrencyEnforcer).enforce(any(InUser.class), anyString());

        assertThrows(OAuth2AuthenticationException.class, () -> registrar.register(user(), registration()));

        verify(onlineTokenService, never()).save(any(InUser.class), any(OnlineSessionRegistration.class));
    }

    private OnlineSessionRegistration registration() {
        Instant now = Instant.now();
        return new OnlineSessionRegistration(SID, "jwt-1", now.plusSeconds(60), now.plusSeconds(600));
    }

    private InUser user() {
        return InUser.stateless(USER_ID, TENANT_ID, CLIENT_ID, TokenAuthTypeEnum.UNIQUE.getValue(),
                UserTypeEnum.ADMIN.getValue(), "admin", List.of(), List.of(), null);
    }
}
