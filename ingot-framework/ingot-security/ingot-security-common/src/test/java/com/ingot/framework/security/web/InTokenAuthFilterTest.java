package com.ingot.framework.security.web;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ingot.framework.commons.constants.PermissionConstants;
import com.ingot.framework.commons.constants.RoleConstants;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.security.core.context.SessionContextHolder;
import com.ingot.framework.security.core.userdetails.InUser;
import com.ingot.framework.security.oauth2.jwt.JwtClaimNamesExtension;
import com.ingot.framework.security.oauth2.server.resource.access.expression.TrustedAuthoritySource;
import com.ingot.framework.security.oauth2.server.resource.authentication.InJwtAuthenticationToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * <p>改密门禁覆盖仅登录及管理员请求，精确允许密码方法，公开入口及正常身份保持原行为。</p>
 * @author jy
 * @since 1.0.0
 */
class InTokenAuthFilterTest {
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void livePasswordFlagBlocksBusinessAndAuthenticatedOnlyEvenWithAdministratorAuthority() throws Exception {
        authenticate(List.of(RoleConstants.ROLE_ADMIN_CODE));
        var source = mock(TrustedAuthoritySource.class);
        when(source.requiresPasswordChange()).thenReturn(true);
        for (String path : List.of("/v1/platform/members", "/v1/me/bootstrap", "/v1/me/profile", "/v1/me/capabilities")) {
            var response = perform(source, "GET", path);
            assertEquals(403, response.getStatus());
            assertTrue(response.getContentAsString().contains("PasswordChangeRequired"));
        }
        assertNull(SessionContextHolder.get());
    }

    @Test void onlyPasswordGetAndPutAreAllowedWhilePublicStillWorks() throws Exception {
        authenticate(List.of(PermissionConstants.INIT_PASSWORD));
        var source = mock(TrustedAuthoritySource.class);
        when(source.requiresPasswordChange()).thenReturn(true);
        assertEquals(204, perform(source, "GET", "/v1/me/password").getStatus());
        assertEquals(204, perform(source, "PUT", "/v1/me/password").getStatus());
        assertEquals(403, perform(source, "POST", "/v1/me/password").getStatus());
        SecurityContextHolder.clearContext();
        assertEquals(204, perform(source, "GET", "/public").getStatus());
    }

    @Test void missingOrFailedTrustedStateFailsClosedAndNormalRequestWorks() throws Exception {
        authenticate(List.of(RoleConstants.ROLE_ADMIN_CODE));
        assertEquals(503, perform(null, "GET", "/v1/platform/members").getStatus());
        var source = mock(TrustedAuthoritySource.class);
        when(source.requiresPasswordChange()).thenThrow(new IllegalStateException("offline"));
        assertEquals(503, perform(source, "GET", "/v1/me/profile").getStatus());
        doReturn(false).when(source).requiresPasswordChange();
        assertEquals(204, perform(source, "GET", "/v1/platform/members").getStatus());
    }

    @Test void sessionStoreFallbackCannotUseTokenAloneForAuthenticatedOnlyEndpoint() throws Exception {
        var jwt = Jwt.withTokenValue("test").header("alg", "none").subject("test")
                .claim(JwtClaimNamesExtension.SID, "sid").build();
        var user = InUser.stateless(1L, null, "web", null, null, "user", List.of(), List.of(), Map.of());
        SecurityContextHolder.getContext().setAuthentication(new InJwtAuthenticationToken(jwt, user, List.of()));
        assertEquals(503, perform(null, "GET", "/v1/me/profile").getStatus());
    }

    @Test void passwordEndpointStillRequiresAuthentication() throws Exception {
        SecurityContextHolder.clearContext();
        assertThrows(org.springframework.security.oauth2.core.OAuth2AuthenticationException.class,
                () -> perform(null, "GET", "/v1/me/password"));
    }

    @Test void sessionPasswordMarkerCannotBeBypassedWithLiveFalse() throws Exception {
        authenticate(List.of("SCOPE_" + PermissionConstants.INIT_PASSWORD));
        assertEquals(403, perform(mock(TrustedAuthoritySource.class), "GET", "/v1/me/profile").getStatus());
    }

    @SuppressWarnings("unchecked")
    private MockHttpServletResponse perform(TrustedAuthoritySource source, String method, String path) throws Exception {
        ObjectProvider<TrustedAuthoritySource> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(source);
        var filter = new InTokenAuthFilter(request -> request.getRequestURI().equals("/public"),
                request -> request.getRequestURI().equals("/v1/me/password")
                        && Set.of("GET", "PUT").contains(request.getMethod()), provider);
        var response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest(method, path), response,
                (request, result) -> ((jakarta.servlet.http.HttpServletResponse) result).setStatus(204));
        return response;
    }

    private void authenticate(List<String> authorities) {
        var granted = authorities.stream().map(org.springframework.security.core.authority.SimpleGrantedAuthority::new).toList();
        var user = InUser.stateless(1L, null, "web", "standard", "0", "user", granted, List.of(), Map.of())
                .toBuilder().authorizationContext(new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "1", "2")).build();
        var jwt = Jwt.withTokenValue("test").header("alg", "none").subject("user")
                .claim(JwtClaimNamesExtension.SID, "session").build();
        SecurityContextHolder.getContext().setAuthentication(new InJwtAuthenticationToken(jwt, user, granted));
    }
}
