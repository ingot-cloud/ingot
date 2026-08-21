package com.ingot.framework.security.oauth2.server.authorization.session.concurrency;

import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

import com.ingot.framework.commons.model.security.SessionConcurrencyDimension;
import com.ingot.framework.commons.model.security.SessionOverflowStrategy;
import com.ingot.framework.commons.model.security.SessionRevokeReason;
import com.ingot.framework.commons.model.security.TokenAuthTypeEnum;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.core.userdetails.InUser;
import com.ingot.framework.security.oauth2.server.authorization.OnlineToken;
import com.ingot.framework.security.oauth2.server.authorization.OnlineTokenService;
import com.ingot.framework.security.oauth2.server.authorization.session.SessionRevocationService;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link SessionConcurrencyEnforcer} 并发约束执行行为。
 *
 * @author jy
 * @since 1.0.0
 */
class SessionConcurrencyEnforcerTest {

    private static final long TENANT_ID = 1L;
    private static final String CLIENT_ID = "web";
    private static final long USER_ID = 9L;
    private static final String CURRENT_SID = "session-current";

    private final OnlineTokenService onlineTokenService = mock(OnlineTokenService.class);
    private final SessionRevocationService revocationService = mock(SessionRevocationService.class);

    @Test
    void unlimitedPolicy_standardClient_keepsAllSessions() {
        givenOnlineSessions(5);
        SessionConcurrencyEnforcer enforcer = enforcer(SessionConcurrencyRule.unlimited());

        enforcer.enforce(user(TokenAuthTypeEnum.STANDARD, UserTypeEnum.ADMIN), CURRENT_SID);

        verify(revocationService, never()).revokeBySid(anyString(), any(SessionRevokeReason.class));
    }

    @Test
    void unlimitedPolicy_uniqueClient_defaultsToSingleSession() {
        givenOnlineSessions(1);
        SessionConcurrencyEnforcer enforcer = enforcer(SessionConcurrencyRule.unlimited());

        enforcer.enforce(user(TokenAuthTypeEnum.UNIQUE, UserTypeEnum.ADMIN), CURRENT_SID);

        verify(revocationService).revokeBySid("session-0", SessionRevokeReason.CONCURRENT_KICKOUT);
    }

    @Test
    void explicitPolicy_overridesUniqueClientDefault() {
        givenOnlineSessions(2);
        SessionConcurrencyEnforcer enforcer = enforcer(rule(3, SessionOverflowStrategy.KICK_OLDEST, false));

        enforcer.enforce(user(TokenAuthTypeEnum.UNIQUE, UserTypeEnum.ADMIN), CURRENT_SID);

        verify(revocationService, never()).revokeBySid(anyString(), any(SessionRevokeReason.class));
    }

    @Test
    void currentSessionNotCountedTowardsLimit() {
        when(onlineTokenService.listUserSessions(TENANT_ID, CLIENT_ID, USER_ID))
                .thenReturn(List.of(session(CURRENT_SID, 100)));
        SessionConcurrencyEnforcer enforcer = enforcer(rule(1, SessionOverflowStrategy.REJECT, false));

        enforcer.enforce(user(TokenAuthTypeEnum.STANDARD, UserTypeEnum.ADMIN), CURRENT_SID);

        verify(revocationService, never()).revokeBySid(anyString(), any(SessionRevokeReason.class));
    }

    @Test
    void overflowReject_refusesLogin() {
        givenOnlineSessions(2);
        SessionConcurrencyEnforcer enforcer = enforcer(rule(2, SessionOverflowStrategy.REJECT, false));

        OAuth2AuthenticationException e = assertThrows(OAuth2AuthenticationException.class,
                () -> enforcer.enforce(user(TokenAuthTypeEnum.STANDARD, UserTypeEnum.ADMIN), CURRENT_SID));

        assertEquals("concurrent_session_limit", e.getError().getErrorCode());
        verify(revocationService, never()).revokeBySid(anyString(), any(SessionRevokeReason.class));
    }

    @Test
    void overflowKickOldest_freesExactlyOneSlot() {
        givenOnlineSessions(4);
        SessionConcurrencyEnforcer enforcer = enforcer(rule(2, SessionOverflowStrategy.KICK_OLDEST, false));

        enforcer.enforce(user(TokenAuthTypeEnum.STANDARD, UserTypeEnum.ADMIN), CURRENT_SID);

        // 4 个在线、上限 2，需腾出 1 个名额 → 踢除最旧的 3 个
        verify(revocationService).revokeBySid("session-0", SessionRevokeReason.CONCURRENT_KICKOUT);
        verify(revocationService).revokeBySid("session-1", SessionRevokeReason.CONCURRENT_KICKOUT);
        verify(revocationService).revokeBySid("session-2", SessionRevokeReason.CONCURRENT_KICKOUT);
        verify(revocationService, never()).revokeBySid("session-3", SessionRevokeReason.CONCURRENT_KICKOUT);
    }

    @Test
    void overflowKickAll_revokesEveryOtherSession() {
        givenOnlineSessions(3);
        SessionConcurrencyEnforcer enforcer = enforcer(rule(2, SessionOverflowStrategy.KICK_ALL, false));

        enforcer.enforce(user(TokenAuthTypeEnum.STANDARD, UserTypeEnum.ADMIN), CURRENT_SID);

        verify(revocationService).revokeBySid("session-0", SessionRevokeReason.CONCURRENT_KICKOUT);
        verify(revocationService).revokeBySid("session-1", SessionRevokeReason.CONCURRENT_KICKOUT);
        verify(revocationService).revokeBySid("session-2", SessionRevokeReason.CONCURRENT_KICKOUT);
    }

    @Test
    void adminForbidConcurrent_forcesSingleSessionForAdminUser() {
        givenOnlineSessions(1);
        SessionConcurrencyEnforcer enforcer = enforcer(rule(5, SessionOverflowStrategy.KICK_OLDEST, true));

        enforcer.enforce(user(TokenAuthTypeEnum.STANDARD, UserTypeEnum.ADMIN), CURRENT_SID);

        verify(revocationService).revokeBySid("session-0", SessionRevokeReason.CONCURRENT_KICKOUT);
    }

    @Test
    void adminForbidConcurrent_doesNotAffectAppUser() {
        givenOnlineSessions(1);
        SessionConcurrencyEnforcer enforcer = enforcer(rule(5, SessionOverflowStrategy.KICK_OLDEST, true));

        enforcer.enforce(user(TokenAuthTypeEnum.STANDARD, UserTypeEnum.APP), CURRENT_SID);

        verify(revocationService, never()).revokeBySid(anyString(), any(SessionRevokeReason.class));
    }

    @Test
    void policyUnavailable_failsClosed() {
        SessionConcurrencyEnforcer enforcer = new SessionConcurrencyEnforcer(
                onlineTokenService, revocationService,
                user -> {
                    throw new SessionPolicyUnavailableException("remote down");
                });

        OAuth2AuthenticationException e = assertThrows(OAuth2AuthenticationException.class,
                () -> enforcer.enforce(user(TokenAuthTypeEnum.STANDARD, UserTypeEnum.ADMIN), CURRENT_SID));

        assertEquals("session_policy_unavailable", e.getError().getErrorCode());
        verify(onlineTokenService, never()).listUserSessions(TENANT_ID, CLIENT_ID, USER_ID);
    }

    private SessionConcurrencyEnforcer enforcer(SessionConcurrencyRule rule) {
        return new SessionConcurrencyEnforcer(onlineTokenService, revocationService, user -> rule);
    }

    private static SessionConcurrencyRule rule(int maxSessions,
                                               SessionOverflowStrategy overflow,
                                               boolean adminForbidConcurrent) {
        return new SessionConcurrencyRule(maxSessions, SessionConcurrencyDimension.USER_CLIENT,
                overflow, adminForbidConcurrent);
    }

    /**
     * 模拟按创建时间倒序返回的在线会话：{@code session-0} 最旧。
     */
    private void givenOnlineSessions(int count) {
        List<OnlineToken> sessions = IntStream.range(0, count)
                .map(i -> count - 1 - i)
                .mapToObj(i -> session("session-" + i, i))
                .toList();
        when(onlineTokenService.listUserSessions(TENANT_ID, CLIENT_ID, USER_ID)).thenReturn(sessions);
    }

    private static OnlineToken session(String sid, int ageOffsetSeconds) {
        return OnlineToken.builder()
                .sid(sid)
                .userId(USER_ID)
                .tenantId(TENANT_ID)
                .clientId(CLIENT_ID)
                .issuedAt(Instant.now().plusSeconds(ageOffsetSeconds))
                .build();
    }

    private static InUser user(TokenAuthTypeEnum authType, UserTypeEnum userType) {
        return InUser.stateless(USER_ID, TENANT_ID, CLIENT_ID, authType.getValue(),
                userType.getValue(), "admin", List.of(), List.of(), null);
    }
}
