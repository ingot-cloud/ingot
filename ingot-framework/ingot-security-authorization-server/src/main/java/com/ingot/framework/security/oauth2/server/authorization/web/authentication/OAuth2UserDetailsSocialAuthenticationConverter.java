package com.ingot.framework.security.oauth2.server.authorization.web.authentication;

import javax.servlet.http.HttpServletRequest;

import com.ingot.framework.security.common.utils.SocialUtils;
import com.ingot.framework.security.core.userdetails.UserDetailsAuthorizationGrantType;
import com.ingot.framework.security.oauth2.core.endpoint.IngotOAuth2ParameterNames;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2UserDetailsAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.util.MultiValueMap;

/**
 * <p>Description  : OAuth2UserDetailsSocialAuthenticationConverter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/11/28.</p>
 * <p>Time         : 12:00 PM.</p>
 */
public class OAuth2UserDetailsSocialAuthenticationConverter extends OAuth2UserDetailsAuthenticationConverter {
    @Override
    protected AuthorizationGrantType getGrantType() {
        return UserDetailsAuthorizationGrantType.SOCIAL;
    }

    @Override
    protected Authentication createUnauthenticated(HttpServletRequest request, Authentication clientPrincipal) {
        MultiValueMap<String, String> parameters = OAuth2EndpointUtils.getParameters(request);

        String socialType = getParameter(parameters, IngotOAuth2ParameterNames.SOCIAL_TYPE);
        String code = getParameter(parameters, IngotOAuth2ParameterNames.SOCIAL_CODE);

        return OAuth2UserDetailsAuthenticationToken
                .unauthenticated(SocialUtils.uniqueCode(socialType, code),
                        null, getGrantType(), clientPrincipal);
    }
}