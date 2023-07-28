package com.ingot.framework.security.oauth2.server.authorization.web.authentication;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.security.common.utils.SocialUtils;
import com.ingot.framework.security.oauth2.core.endpoint.IngotOAuth2ParameterNames;
import com.ingot.framework.security.oauth2.core.endpoint.OAuth2PreAuthorizationType;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2PreAuthorizationAuthenticationToken;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2UserDetailsAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.MultiValueMap;

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

        // preAuthorization (REQUIRED)
        String preAuthorization = request.getParameter(IngotOAuth2ParameterNames.PRE_AUTHORIZATION);
        if (StrUtil.equals(preAuthorization, OAuth2PreAuthorizationType.PASSWORD_CODE.getValue())) {
            return createPassword(request, preAuthorization, userPrincipal);
        }

        if (StrUtil.equals(preAuthorization, OAuth2PreAuthorizationType.SOCIAL_CODE.getValue())) {
            return createSocial(request, preAuthorization, userPrincipal);
        }

        return null;
    }

    private Authentication createPassword(HttpServletRequest request,
                                          String preAuthorization,
                                          OAuth2UserDetailsAuthenticationToken userPrincipal) {
        MultiValueMap<String, String> parameters = OAuth2EndpointUtils.getParameters(request);

        String username = OAuth2EndpointUtils.getParameter(parameters, OAuth2ParameterNames.USERNAME);
        String password = OAuth2EndpointUtils.getParameter(parameters, OAuth2ParameterNames.PASSWORD);
        return OAuth2PreAuthorizationAuthenticationToken.unauthenticated(
                username, password, preAuthorization, userPrincipal);
    }

    private Authentication createSocial(HttpServletRequest request,
                                        String preAuthorization,
                                        OAuth2UserDetailsAuthenticationToken userPrincipal) {
        MultiValueMap<String, String> parameters = OAuth2EndpointUtils.getParameters(request);

        String socialType = OAuth2EndpointUtils.getParameter(parameters, IngotOAuth2ParameterNames.SOCIAL_TYPE);
        String code = OAuth2EndpointUtils.getParameter(parameters, IngotOAuth2ParameterNames.SOCIAL_CODE);
        return OAuth2PreAuthorizationAuthenticationToken
                .unauthenticated(SocialUtils.uniqueCode(socialType, code),
                        null, preAuthorization, userPrincipal);
    }
}
