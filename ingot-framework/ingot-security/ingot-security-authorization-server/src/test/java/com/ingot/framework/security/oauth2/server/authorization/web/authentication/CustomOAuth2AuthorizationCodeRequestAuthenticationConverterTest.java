package com.ingot.framework.security.oauth2.server.authorization.web.authentication;

import java.util.List;
import java.util.Map;

import com.ingot.framework.commons.model.common.TenantMainDTO;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.security.core.userdetails.InUser;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2PreAuthorizationCodeRequestAuthenticationToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationException;

import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomOAuth2AuthorizationCodeRequestAuthenticationConverterTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void platformAuthorizeRejectsOrg() {
        CustomOAuth2AuthorizationCodeRequestAuthenticationConverter converter =
                new CustomOAuth2AuthorizationCodeRequestAuthenticationConverter();
        MockHttpServletRequest request = authorizeRequest();
        request.setParameter("org", "1000001");
        SecurityContextHolder.getContext().setAuthentication(platformPreAuth());
        assertThrows(OAuth2AuthorizationCodeRequestAuthenticationException.class, () -> converter.convert(request));
    }

    @Test
    void tenantAuthorizeRequiresOrg() {
        CustomOAuth2AuthorizationCodeRequestAuthenticationConverter converter =
                new CustomOAuth2AuthorizationCodeRequestAuthenticationConverter();
        MockHttpServletRequest request = authorizeRequest();
        SecurityContextHolder.getContext().setAuthentication(tenantPreAuth());
        assertThrows(OAuth2AuthorizationCodeRequestAuthenticationException.class, () -> converter.convert(request));
    }

    private static MockHttpServletRequest authorizeRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("client_id", "ingot-bff");
        request.setParameter("response_type", "code");
        request.setParameter("redirect_uri", "http://localhost:5400/bff/auth/callback");
        request.setParameter("code_challenge", "abc");
        return request;
    }

    private static OAuth2PreAuthorizationCodeRequestAuthenticationToken platformPreAuth() {
        InUser user = new InUser(1L, null, "ingot-bff", "standard", "0", "platform", "n",
                true, true, true, true, List.of(), null, null, null,
                new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "1", "9"));
        return OAuth2PreAuthorizationCodeRequestAuthenticationToken.authenticated(
                user, List.of(), boundParams(), 3600);
    }

    private static OAuth2PreAuthorizationCodeRequestAuthenticationToken tenantPreAuth() {
        InUser user = new InUser(1L, null, "ingot-bff", "standard", "0", "owner", "n",
                true, true, true, true, List.of(), null, null, null);
        TenantMainDTO allow = new TenantMainDTO();
        allow.setId("1000001");
        return OAuth2PreAuthorizationCodeRequestAuthenticationToken.authenticated(
                user, List.of(allow), boundParams(), 3600);
    }

    private static Map<String, Object> boundParams() {
        return Map.of(
                "client_id", "ingot-bff",
                "redirect_uri", "http://localhost:5400/bff/auth/callback",
                "code_challenge", "abc");
    }
}
