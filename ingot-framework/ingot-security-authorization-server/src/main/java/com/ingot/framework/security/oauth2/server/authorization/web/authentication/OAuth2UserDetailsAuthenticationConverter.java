package com.ingot.framework.security.oauth2.server.authorization.web.authentication;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2UserDetailsAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.MultiValueMap;

import javax.servlet.http.HttpServletRequest;

/**
 * <p>Description  : {@link OAuth2UserDetailsAuthenticationToken}转换器.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/9.</p>
 * <p>Time         : 5:56 下午.</p>
 */
public abstract class OAuth2UserDetailsAuthenticationConverter implements AuthenticationConverter {

    @Override
    public Authentication convert(HttpServletRequest request) {
        // grant_type (REQUIRED)
        String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
        if (!StrUtil.equals(getGrantType().getValue(), grantType)) {
            return null;
        }

        Authentication clientPrincipal = SecurityContextHolder.getContext().getAuthentication();
        return createUnauthenticated(request, clientPrincipal);
    }

    protected abstract AuthorizationGrantType getGrantType();

    protected Authentication createUnauthenticated(HttpServletRequest request, Authentication clientPrincipal) {
        MultiValueMap<String, String> parameters = OAuth2EndpointUtils.getParameters(request);

        String username = OAuth2EndpointUtils.getParameter(parameters, OAuth2ParameterNames.USERNAME);
        String password = OAuth2EndpointUtils.getParameter(parameters, OAuth2ParameterNames.PASSWORD);
        return OAuth2UserDetailsAuthenticationToken
                .unauthenticated(username, password, getGrantType(), clientPrincipal);
    }
}
