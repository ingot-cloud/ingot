package com.ingot.framework.security.oauth2.server.authorization.web.authentication;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.core.constants.InOAuth2ParameterNames;
import com.ingot.framework.security.oauth2.core.endpoint.PreAuthorizationGrantType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationConverter;

/**
 * <p>Description  : 预授权模式，用户信息认证转换器.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/28.</p>
 * <p>Time         : 2:17 PM.</p>
 */
public class OAuth2PreAuthorizationCodeRequestUserDetailsAuthenticationConverter implements AuthenticationConverter {
    private final OAuth2UserDetailsPasswordAuthenticationConverter passwordConverter = new OAuth2UserDetailsPasswordAuthenticationConverter();
    private final OAuth2UserDetailsSocialAuthenticationConverter socialConverter = new OAuth2UserDetailsSocialAuthenticationConverter();

    @Override
    public Authentication convert(HttpServletRequest request) {
        // Must post
        if (!"POST".equals(request.getMethod())) {
            return null;
        }

        // 如果持有已经认证的OAuth2PreAuthorizationCodeRequestAuthenticationToken, 那么不进行用户认证
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (OAuth2PreAuthorizationUtils.hadOAuth2PreAuthorizationCodeRequestAuthenticationToken(authentication, request)) {
            return null;
        }

        // pre_grant_type (REQUIRED)
        String preGrantType = request.getParameter(InOAuth2ParameterNames.PRE_GRANT_TYPE);
        if (StrUtil.equals(preGrantType, PreAuthorizationGrantType.PASSWORD.value())) {
            return passwordConverter.createUnauthenticated(request, authentication);
        }

        if (StrUtil.equals(preGrantType, PreAuthorizationGrantType.SOCIAL.value())) {
            return socialConverter.createUnauthenticated(request, authentication);
        }

        return null;
    }
}
