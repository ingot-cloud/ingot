package com.ingot.framework.security.oauth2.server.authorization.web.authentication;

import java.util.Map;
import java.util.stream.Collectors;

import cn.hutool.core.collection.ListUtil;
import com.ingot.framework.security.oauth2.core.InAuthorizationGrantType;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2CustomAuthenticationToken;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2UserDetailsAuthenticationToken;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.MultiValueMap;

/**
 * <p>Description  : 自定义OAuth2认证转换器.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/8.</p>
 * <p>Time         : 5:15 下午.</p>
 */
@Slf4j
public final class OAuth2CustomAuthenticationConverter implements AuthenticationConverter {

    @Override
    public Authentication convert(HttpServletRequest request) {
        // grant_type (REQUIRED)
        String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
        if (!filter(grantType)) {
            return null;
        }

        OAuth2UserDetailsAuthenticationToken userPrincipal =
                (OAuth2UserDetailsAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        MultiValueMap<String, String> parameters = OAuth2EndpointUtils.getParameters(request);

        Map<String, Object> additionalParameters = parameters
                .entrySet()
                .stream()
                .filter(entry -> !entry.getKey().equals(OAuth2ParameterNames.GRANT_TYPE)
                        && !entry.getKey().equals(OAuth2ParameterNames.SCOPE))
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0)));

        return new OAuth2CustomAuthenticationToken(
                userPrincipal, userPrincipal.getClient(), additionalParameters);
    }

    private boolean filter(String grantType) {
        return ListUtil.list(false,
                        InAuthorizationGrantType.PASSWORD.getValue(),
                        InAuthorizationGrantType.SOCIAL.getValue())
                .contains(grantType);
    }
}
