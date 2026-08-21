package com.ingot.framework.security.oauth2.server.resource.authentication;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.ingot.framework.commons.model.security.TokenAuthTypeEnum;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.core.userdetails.InUser;
import com.ingot.framework.security.oauth2.jwt.JwtClaimNamesExtension;
import com.ingot.framework.security.oauth2.server.authorization.OnlineToken;
import com.ingot.framework.security.oauth2.server.authorization.OnlineTokenService;
import com.ingot.framework.security.oauth2.server.authorization.SessionStoreAvailability;
import org.junit.jupiter.api.Test;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link JwtInUserConverter} 会话权威性与 Redis 故障宽限行为。
 *
 * @author jy
 * @since 1.0.0
 */
class JwtInUserConverterTest {

    private static final String SID = "session-1";
    private static final String CLIENT_ID = "web";
    private static final long USER_ID = 9L;
    private static final long TENANT_ID = 1L;

    private final OnlineTokenService onlineTokenService = mock(OnlineTokenService.class);

    @Test
    void sessionHit_mergesSessionAuthorities() {
        when(onlineTokenService.getBySid(SID)).thenReturn(Optional.of(session()));
        JwtInUserConverter converter = converter(Duration.ofSeconds(30));

        InUser user = converter.convert(jwt(SID));

        assertEquals(USER_ID, user.getId());
        assertEquals(TENANT_ID, user.getTenantId());
        assertEquals(UserTypeEnum.ADMIN.getValue(), user.getUserType());
        assertEquals(TokenAuthTypeEnum.UNIQUE.getValue(), user.getTokenAuthType());
        assertEquals(List.of(2L), user.getDeptIds());
        assertTrue(user.getAuthorities().stream().anyMatch(
                authority -> (InJwtAuthenticationConverter.AUTHORITY_PREFIX + "r_admin")
                        .equals(authority.getAuthority())));
        assertTrue(user.getAuthorities().stream().anyMatch(
                authority -> (InJwtAuthenticationConverter.AUTHORITY_PREFIX + "server").
                        equals(authority.getAuthority())));
    }

    @Test
    void missingSidClaim_rejects() {
        JwtInUserConverter converter = converter(Duration.ofSeconds(30));

        assertThrows(OAuth2AuthenticationException.class, () -> converter.convert(jwt(null)));
    }

    @Test
    void sessionRevoked_rejects() {
        when(onlineTokenService.getBySid(SID)).thenReturn(Optional.empty());
        JwtInUserConverter converter = converter(Duration.ofSeconds(30));

        assertThrows(OAuth2AuthenticationException.class, () -> converter.convert(jwt(SID)));
    }

    @Test
    void storeUnavailableWithinGrace_degradesToScopeOnly() {
        when(onlineTokenService.getBySid(anyString())).thenThrow(new QueryTimeoutException("redis timeout"));
        JwtInUserConverter converter = converter(Duration.ofSeconds(30));

        InUser user = converter.convert(jwt(SID));

        assertEquals(USER_ID, user.getId());
        assertNull(user.getUserType());
        assertNull(user.getTokenAuthType());
        assertEquals(1, user.getAuthorities().size());
    }

    @Test
    void storeUnavailableBeyondGrace_rejects() {
        when(onlineTokenService.getBySid(anyString())).thenThrow(new QueryTimeoutException("redis timeout"));
        JwtInUserConverter converter = converter(Duration.ZERO);

        // 第一次失败开启宽限计时并放行，超出窗口后转为拒绝
        converter.convert(jwt(SID));
        assertThrows(OAuth2AuthenticationException.class, () -> converter.convert(jwt(SID)));
    }

    private JwtInUserConverter converter(Duration grace) {
        return new JwtInUserConverter(onlineTokenService, new SessionStoreAvailability(grace));
    }

    private OnlineToken session() {
        return OnlineToken.builder()
                .sid(SID)
                .jti("jwt-1")
                .userId(USER_ID)
                .tenantId(TENANT_ID)
                .clientId(CLIENT_ID)
                .principalName("admin")
                .authType(TokenAuthTypeEnum.UNIQUE.getValue())
                .userType(UserTypeEnum.ADMIN.getValue())
                .authorities(Set.of("r_admin"))
                .deptIds(List.of(2L))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
    }

    private Jwt jwt(String sid) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim(JwtClaimNamesExtension.SUB, "admin")
                .claim(JwtClaimNamesExtension.AUD, new ArrayList<>(List.of(CLIENT_ID)))
                .claim(JwtClaimNamesExtension.ID, USER_ID)
                .claim(JwtClaimNamesExtension.TENANT, TENANT_ID)
                .claim(JwtClaimNamesExtension.SCOPE, List.of("server"));
        if (sid != null) {
            builder.claim(JwtClaimNamesExtension.SID, sid);
        }
        return builder.build();
    }
}
