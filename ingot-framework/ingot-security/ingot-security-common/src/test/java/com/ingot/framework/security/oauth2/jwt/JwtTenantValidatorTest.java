package com.ingot.framework.security.oauth2.jwt;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.ingot.framework.commons.constants.HeaderConstants;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.security.core.InSecurityProperties;
import com.ingot.framework.security.oauth2.server.authorization.OnlineToken;
import com.ingot.framework.security.oauth2.server.authorization.OnlineTokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * <p>平台身份允许空 {@code org}；租户身份仍比对请求头。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class JwtTenantValidatorTest {
    private static final String SID = "session-1";
    private static final String CLIENT_ID = "ingot";
    private static final long USER_ID = 900001L;
    private static final long TENANT_ID = 11L;

    private final OnlineTokenService onlineTokenService = mock(OnlineTokenService.class);

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void platformSessionWithoutOrgSucceeds() {
        when(onlineTokenService.getBySid(SID)).thenReturn(Optional.of(platformSession()));
        OAuth2TokenValidatorResult result = validator().validate(jwtWithoutOrg());
        assertFalse(result.hasErrors());
    }

    @Test
    void platformSessionRejectsOrgClaim() {
        when(onlineTokenService.getBySid(SID)).thenReturn(Optional.of(platformSession()));
        OAuth2TokenValidatorResult result = validator().validate(jwtWithOrg(TENANT_ID));
        assertTrue(result.hasErrors());
        assertEquals(OAuth2ErrorCodes.INVALID_TOKEN, result.getErrors().iterator().next().getErrorCode());
    }

    @Test
    void tenantSessionRequiresMatchingTenantHeader() {
        when(onlineTokenService.getBySid(SID)).thenReturn(Optional.of(tenantSession()));
        bindTenantHeader(String.valueOf(TENANT_ID));
        assertFalse(validator().validate(jwtWithOrg(TENANT_ID)).hasErrors());
    }

    @Test
    void tenantSessionRejectsMissingOrg() {
        when(onlineTokenService.getBySid(SID)).thenReturn(Optional.of(tenantSession()));
        bindTenantHeader(String.valueOf(TENANT_ID));
        assertTrue(validator().validate(jwtWithoutOrg()).hasErrors());
    }

    @Test
    void tenantSessionRejectsMismatchedHeader() {
        when(onlineTokenService.getBySid(SID)).thenReturn(Optional.of(tenantSession()));
        bindTenantHeader("99");
        assertTrue(validator().validate(jwtWithOrg(TENANT_ID)).hasErrors());
    }

    @Test
    void missingSessionRejectsEmptyOrg() {
        when(onlineTokenService.getBySid(SID)).thenReturn(Optional.empty());
        assertTrue(validator().validate(jwtWithoutOrg()).hasErrors());
    }

    @Test
    void ignoreRoleStillSkipsTenantCheck() {
        InSecurityProperties properties = new InSecurityProperties();
        properties.setIgnoreTenantValidateRoleCodeList(List.of("role_admin"));
        when(onlineTokenService.getBySid(SID)).thenReturn(Optional.empty());
        Jwt jwt = jwtWithoutOrgBuilder()
                .claim(JwtClaimNamesExtension.SCOPE, List.of("role_admin"))
                .build();
        JwtTenantValidator validator = new JwtTenantValidator(properties, onlineTokenService);
        assertFalse(validator.validate(jwt).hasErrors());
    }

    private JwtTenantValidator validator() {
        return new JwtTenantValidator(new InSecurityProperties(), onlineTokenService);
    }

    private OnlineToken platformSession() {
        return OnlineToken.builder()
                .sid(SID)
                .userId(USER_ID)
                .tenantId(null)
                .clientId(CLIENT_ID)
                .authorizationContext(new AuthorizationContext(
                        AuthorizationDomain.PLATFORM, null, Long.toString(USER_ID), "910001"))
                .authorities(Set.of())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
    }

    private OnlineToken tenantSession() {
        return OnlineToken.builder()
                .sid(SID)
                .userId(USER_ID)
                .tenantId(TENANT_ID)
                .clientId(CLIENT_ID)
                .authorizationContext(new AuthorizationContext(
                        AuthorizationDomain.TENANT, Long.toString(TENANT_ID), Long.toString(USER_ID), "920001"))
                .authorities(Set.of())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
    }

    private Jwt jwtWithoutOrg() {
        return jwtWithoutOrgBuilder().build();
    }

    private Jwt.Builder jwtWithoutOrgBuilder() {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim(JwtClaimNamesExtension.SUB, "platform")
                .claim(JwtClaimNamesExtension.AUD, new ArrayList<>(List.of(CLIENT_ID)))
                .claim(JwtClaimNamesExtension.ID, USER_ID)
                .claim(JwtClaimNamesExtension.SID, SID)
                .claim(JwtClaimNamesExtension.SCOPE, List.of("system"));
    }

    private Jwt jwtWithOrg(long tenantId) {
        return jwtWithoutOrgBuilder()
                .claim(JwtClaimNamesExtension.TENANT, tenantId)
                .build();
    }

    private void bindTenantHeader(String tenantId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HeaderConstants.TENANT, tenantId);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
