package com.ingot.framework.security.oauth2.server.authorization.web.authentication;

import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2PasswordAuthenticationToken;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2UsernamePasswordAuthenticationToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.MultiValueMap;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>Description  : OAuth2PasswordAuthenticationConverter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/8.</p>
 * <p>Time         : 5:15 下午.</p>
 */
@Slf4j
public final class OAuth2PasswordAuthenticationConverter implements AuthenticationConverter {

    @Override
    public Authentication convert(HttpServletRequest request) {
        // grant_type (REQUIRED)
        String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
        if (!AuthorizationGrantType.PASSWORD.getValue().equals(grantType)) {
            return null;
        }

        OAuth2UsernamePasswordAuthenticationToken userPrincipal =
                (OAuth2UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        MultiValueMap<String, String> parameters = OAuth2EndpointUtils.getParameters(request);

        Map<String, Object> additionalParameters = parameters
                .entrySet()
                .stream()
                .filter(entry -> !entry.getKey().equals(OAuth2ParameterNames.GRANT_TYPE)
                        && !entry.getKey().equals(OAuth2ParameterNames.SCOPE))
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0)));

        return new OAuth2PasswordAuthenticationToken(
                userPrincipal, userPrincipal.getClientPrincipal(), additionalParameters);
    }
}
