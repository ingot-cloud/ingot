package com.ingot.framework.security.oauth2.server.authorization.web.authentication;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.security.common.constants.UserType;
import com.ingot.framework.security.core.userdetails.UsernameUri;
import com.ingot.framework.security.oauth2.core.OAuth2ErrorUtils;
import com.ingot.framework.security.oauth2.core.endpoint.IngotOAuth2ParameterNames;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2UserDetailsAuthenticationToken;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.MultiValueMap;

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
        String userTypeValue = OAuth2EndpointUtils.getParameter(parameters, IngotOAuth2ParameterNames.USER_TYPE);
        UserType userType = UserType.getEnum(userTypeValue);
        if (userType == null) {
            OAuth2ErrorUtils.throwInvalidRequestParameter(IngotOAuth2ParameterNames.USER_TYPE);
        }
        String principal = UsernameUri.of(username, userTypeValue, getGrantType().getValue()).getValue();
        return OAuth2UserDetailsAuthenticationToken
                .unauthenticated(principal,
                        password, getGrantType(), clientPrincipal);
    }
}
