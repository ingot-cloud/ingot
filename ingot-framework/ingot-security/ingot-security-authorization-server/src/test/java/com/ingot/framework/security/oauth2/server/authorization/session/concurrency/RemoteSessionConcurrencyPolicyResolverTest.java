package com.ingot.framework.security.oauth2.server.authorization.session.concurrency;

import java.util.List;

import com.ingot.cloud.security.api.model.vo.policy.SessionConcurrencyPolicyVO;
import com.ingot.framework.cache.spi.LayeredCache;
import com.ingot.framework.commons.model.security.SessionConcurrencyDimension;
import com.ingot.framework.commons.model.security.SessionOverflowStrategy;
import com.ingot.framework.commons.model.security.SessionPolicyScope;
import com.ingot.framework.commons.model.security.TokenAuthTypeEnum;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.core.userdetails.InUser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link RemoteSessionConcurrencyPolicyResolver} 策略选取与降级语义。
 *
 * @author jy
 * @since 1.0.0
 */
class RemoteSessionConcurrencyPolicyResolverTest {

    private static final String CLIENT_ID = "web";

    @SuppressWarnings("unchecked")
    private final LayeredCache<String, List<SessionConcurrencyPolicyVO>> cache = mock(LayeredCache.class);

    private final RemoteSessionConcurrencyPolicyResolver resolver =
            new RemoteSessionConcurrencyPolicyResolver(cache);

    @Test
    void clientScope_winsOverUserTypeAndGlobal() {
        givenPolicies(
                policy(1L, SessionPolicyScope.GLOBAL, "", "", 9),
                policy(2L, SessionPolicyScope.USER_TYPE, "", UserTypeEnum.ADMIN.getValue(), 5),
                policy(3L, SessionPolicyScope.CLIENT, CLIENT_ID, "", 2));

        assertEquals(2, resolver.resolve(user()).maxSessions());
    }

    @Test
    void userTypeScope_usedWhenNoClientPolicy() {
        givenPolicies(
                policy(1L, SessionPolicyScope.GLOBAL, "", "", 9),
                policy(2L, SessionPolicyScope.USER_TYPE, "", UserTypeEnum.ADMIN.getValue(), 5),
                policy(3L, SessionPolicyScope.CLIENT, "other-client", "", 2));

        assertEquals(5, resolver.resolve(user()).maxSessions());
    }

    @Test
    void globalScope_usedAsFallback() {
        givenPolicies(
                policy(1L, SessionPolicyScope.GLOBAL, "", "", 9),
                policy(2L, SessionPolicyScope.USER_TYPE, "", UserTypeEnum.APP.getValue(), 5));

        assertEquals(9, resolver.resolve(user()).maxSessions());
    }

    @Test
    void disabledPolicy_isIgnored() {
        SessionConcurrencyPolicyVO disabled = policy(3L, SessionPolicyScope.CLIENT, CLIENT_ID, "", 2);
        disabled.setEnabled(Boolean.FALSE);
        givenPolicies(policy(1L, SessionPolicyScope.GLOBAL, "", "", 9), disabled);

        assertEquals(9, resolver.resolve(user()).maxSessions());
    }

    @Test
    void sameScopeDuplicates_takeSmallestId() {
        givenPolicies(
                policy(7L, SessionPolicyScope.CLIENT, CLIENT_ID, "", 7),
                policy(4L, SessionPolicyScope.CLIENT, CLIENT_ID, "", 4));

        assertEquals(4, resolver.resolve(user()).maxSessions());
    }

    @Test
    void emptyPolicies_meanUnlimited() {
        givenPolicies();

        SessionConcurrencyRule rule = resolver.resolve(user());

        assertEquals(0, rule.maxSessions());
        assertEquals(SessionOverflowStrategy.KICK_OLDEST, rule.overflow());
    }

    @Test
    void remoteUnavailable_failsClosed() {
        when(cache.get(RemoteSessionConcurrencyPolicyResolver.CACHE_KEY))
                .thenThrow(new SessionConcurrencyRemoteUnavailableException("center down"));

        assertThrows(SessionPolicyUnavailableException.class, () -> resolver.resolve(user()));
    }

    private void givenPolicies(SessionConcurrencyPolicyVO... policies) {
        when(cache.get(RemoteSessionConcurrencyPolicyResolver.CACHE_KEY)).thenReturn(List.of(policies));
    }

    private static SessionConcurrencyPolicyVO policy(Long id, SessionPolicyScope scope,
                                                    String clientId, String userType, int maxSessions) {
        SessionConcurrencyPolicyVO vo = new SessionConcurrencyPolicyVO();
        vo.setId(id);
        vo.setScope(scope);
        vo.setClientId(clientId);
        vo.setUserType(userType);
        vo.setMaxSessions(maxSessions);
        vo.setDimension(SessionConcurrencyDimension.USER_CLIENT);
        vo.setOverflow(SessionOverflowStrategy.KICK_OLDEST);
        vo.setAdminForbidConcurrent(Boolean.FALSE);
        vo.setEnabled(Boolean.TRUE);
        return vo;
    }

    private static InUser user() {
        return InUser.stateless(9L, 1L, CLIENT_ID, TokenAuthTypeEnum.STANDARD.getValue(),
                UserTypeEnum.ADMIN.getValue(), "admin", List.of(), List.of(), null);
    }
}
