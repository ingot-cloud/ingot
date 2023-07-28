package com.ingot.framework.security.oauth2.server.authorization.web.authentication;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.security.oauth2.core.endpoint.IngotOAuth2ParameterNames;
import com.ingot.framework.security.oauth2.core.endpoint.PreAuthorizationGrantType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationConverter;

import javax.servlet.http.HttpServletRequest;

/**
 * <p>Description  : OAuth2PreAuthorizationUserDetailsAuthenticationConverter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/28.</p>
 * <p>Time         : 2:17 PM.</p>
 */
public class OAuth2PreAuthorizationUserDetailsAuthenticationConverter implements AuthenticationConverter {
    private final OAuth2UserDetailsPasswordAuthenticationConverter passwordConverter = new OAuth2UserDetailsPasswordAuthenticationConverter();
    private final OAuth2UserDetailsSocialAuthenticationConverter socialConverter = new OAuth2UserDetailsSocialAuthenticationConverter();

    @Override
    public Authentication convert(HttpServletRequest request) {
        // Must post
        if (!"POST".equals(request.getMethod())) {
            return null;
        }

        Authentication clientPrincipal = SecurityContextHolder.getContext().getAuthentication();

        // pre_grant_type (REQUIRED)
        String preGrantType = request.getParameter(IngotOAuth2ParameterNames.PRE_GRANT_TYPE);
        if (StrUtil.equals(preGrantType, PreAuthorizationGrantType.PASSWORD.getValue())) {
            return passwordConverter.createUnauthenticated(request, clientPrincipal);
        }

        if (StrUtil.equals(preGrantType, PreAuthorizationGrantType.SOCIAL.getValue())) {
            return socialConverter.createUnauthenticated(request, clientPrincipal);
        }

        return null;
    }
}
