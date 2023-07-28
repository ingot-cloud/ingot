package com.ingot.framework.security.oauth2.server.authorization.web.authentication;

import com.ingot.framework.security.oauth2.core.endpoint.IngotOAuth2ParameterNames;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2PreAuthorizationAuthenticationToken;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2UserDetailsAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationConverter;

import javax.servlet.http.HttpServletRequest;

/**
 * <p>Description  : OAuth2PreAuthorizationAuthenticationConverter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/26.</p>
 * <p>Time         : 11:50 AM.</p>
 */
public final class OAuth2PreAuthorizationAuthenticationConverter implements AuthenticationConverter {

    @Override
    public Authentication convert(HttpServletRequest request) {
        // Must post
        if (!"POST".equals(request.getMethod())) {
            return null;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof OAuth2UserDetailsAuthenticationToken)) {
            return null;
        }

        OAuth2UserDetailsAuthenticationToken userPrincipal =
                (OAuth2UserDetailsAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

        // pre_grant_type (REQUIRED)
        String preGrantType = request.getParameter(IngotOAuth2ParameterNames.PRE_GRANT_TYPE);
        return OAuth2PreAuthorizationAuthenticationToken.unauthenticated(userPrincipal, preGrantType);
    }
}
