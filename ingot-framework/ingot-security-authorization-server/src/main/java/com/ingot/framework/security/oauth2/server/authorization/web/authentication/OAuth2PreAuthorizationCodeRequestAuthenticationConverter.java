package com.ingot.framework.security.oauth2.server.authorization.web.authentication;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import com.ingot.framework.security.oauth2.core.OAuth2ErrorUtils;
import com.ingot.framework.security.oauth2.core.endpoint.IngotOAuth2ParameterNames;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2PreAuthorizationCodeRequestAuthenticationToken;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2UserDetailsAuthenticationToken;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>Description  : Convert request to {@link OAuth2PreAuthorizationCodeRequestAuthenticationToken}.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/26.</p>
 * <p>Time         : 11:50 AM.</p>
 */
@Slf4j
public final class OAuth2PreAuthorizationCodeRequestAuthenticationConverter implements AuthenticationConverter {
    private static final String PKCE_ERROR_URI = "https://datatracker.ietf.org/doc/html/rfc7636#section-4.4.1";

    private static final List<String> requiredParameters = ListUtil.list(false,
            PkceParameterNames.CODE_CHALLENGE,
            OAuth2ParameterNames.CLIENT_ID,
            OAuth2ParameterNames.RESPONSE_TYPE,
            OAuth2ParameterNames.REDIRECT_URI,
            OAuth2ParameterNames.SCOPE
    );
    private static final List<String> savedParameters = ListUtil.list(false,
            PkceParameterNames.CODE_CHALLENGE,
            OAuth2ParameterNames.CLIENT_ID,
            OAuth2ParameterNames.RESPONSE_TYPE,
            OAuth2ParameterNames.REDIRECT_URI,
            OAuth2ParameterNames.SCOPE,
            OAuth2ParameterNames.STATE
    );

    @Override
    public Authentication convert(HttpServletRequest request) {
        // Must post
        if (!"POST".equals(request.getMethod())) {
            return null;
        }

        MultiValueMap<String, String> parameters = OAuth2EndpointUtils.getParameters(request);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof OAuth2UserDetailsAuthenticationToken userDetailsAuthentication)) {
            return null;
        }

        if (!((userDetailsAuthentication).getClient() instanceof OAuth2ClientAuthenticationToken clientAuthentication)) {
            return null;
        }

        // pre_grant_type (REQUIRED)
        String preGrantType = request.getParameter(IngotOAuth2ParameterNames.PRE_GRANT_TYPE);
        if (StrUtil.isEmpty(preGrantType)) {
            OAuth2ErrorUtils.throwInvalidRequestParameter(IngotOAuth2ParameterNames.PRE_GRANT_TYPE, null);
        }

        // state (RECOMMENDED)
        String state = parameters.getFirst(OAuth2ParameterNames.STATE);
        if (StrUtil.isNotEmpty(state) &&
                parameters.get(OAuth2ParameterNames.STATE).size() != 1) {
            OAuth2ErrorUtils.throwInvalidRequestParameter(OAuth2ParameterNames.STATE, null);
        }

        // REQUIRED parameters
        requiredParameters.forEach(field -> {
            if (!parameters.containsKey(field)) {
                OAuth2ErrorUtils.throwInvalidRequestParameter(field, null);
            }
        });

        Map<String, Object> additionalParameters = new HashMap<>();
        parameters.forEach((key, value) -> {
            if (savedParameters.contains(key)) {
                additionalParameters.put(key, (value.size() == 1) ? value.get(0) : value.toArray(new String[0]));
            }
        });

        RegisteredClient client = clientAuthentication.getRegisteredClient();
        return OAuth2PreAuthorizationCodeRequestAuthenticationToken.unauthenticated(
                authentication, preGrantType, client, additionalParameters);
    }
}
