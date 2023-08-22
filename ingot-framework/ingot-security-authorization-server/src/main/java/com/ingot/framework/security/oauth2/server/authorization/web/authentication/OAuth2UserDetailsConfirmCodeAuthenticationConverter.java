package com.ingot.framework.security.oauth2.server.authorization.web.authentication;

import com.ingot.framework.security.core.userdetails.UserDetailsAuthorizationGrantType;
import com.ingot.framework.security.oauth2.core.endpoint.IngotOAuth2ParameterNames;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2UserDetailsAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.util.MultiValueMap;

import jakarta.servlet.http.HttpServletRequest;

/**
 * <p>Description  : OAuth2UserDetailsConfirmCodeAuthenticationConverter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/28.</p>
 * <p>Time         : 3:38 PM.</p>
 */
public class OAuth2UserDetailsConfirmCodeAuthenticationConverter extends OAuth2UserDetailsAuthenticationConverter {
    @Override
    protected AuthorizationGrantType getGrantType() {
        return UserDetailsAuthorizationGrantType.CONFIRM_CODE;
    }

    @Override
    protected Authentication createUnauthenticated(HttpServletRequest request, Authentication clientPrincipal) {
        MultiValueMap<String, String> parameters = OAuth2EndpointUtils.getParameters(request);

        String code = OAuth2EndpointUtils.getParameter(parameters, IngotOAuth2ParameterNames.PRE_CODE);
        return OAuth2UserDetailsAuthenticationToken
                .unauthenticated(code, null, getGrantType(), clientPrincipal);
    }
}
