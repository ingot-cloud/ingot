package com.ingot.framework.security.oauth2.server.authorization.web.authentication;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.security.oauth2.core.OAuth2ErrorUtils;
import com.ingot.framework.security.oauth2.core.endpoint.IngotOAuth2ParameterNames;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2PreAuthorizationCodeRequestAuthenticationToken;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2UserDetailsAuthenticationToken;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Description  : Convert request to {@link OAuth2PreAuthorizationCodeRequestAuthenticationToken}.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/26.</p>
 * <p>Time         : 11:50 AM.</p>
 */
public final class OAuth2PreAuthorizationCodeRequestAuthenticationConverter implements AuthenticationConverter {
    private static final String PKCE_ERROR_URI = "https://datatracker.ietf.org/doc/html/rfc7636#section-4.4.1";

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

        // code_challenge (REQUIRED for public clients) - RFC 7636 (PKCE)
        String codeChallenge = parameters.getFirst(PkceParameterNames.CODE_CHALLENGE);
        if (StrUtil.isNotEmpty(codeChallenge) &&
                parameters.get(PkceParameterNames.CODE_CHALLENGE).size() != 1) {
            OAuth2ErrorUtils.throwInvalidRequestParameter(PkceParameterNames.CODE_CHALLENGE, PKCE_ERROR_URI);
        }

        // code_challenge_method (OPTIONAL for public clients) - RFC 7636 (PKCE)
        String codeChallengeMethod = parameters.getFirst(PkceParameterNames.CODE_CHALLENGE_METHOD);
        if (StrUtil.isNotEmpty(codeChallengeMethod) &&
                parameters.get(PkceParameterNames.CODE_CHALLENGE_METHOD).size() != 1) {
            OAuth2ErrorUtils.throwInvalidRequestParameter(PkceParameterNames.CODE_CHALLENGE_METHOD, PKCE_ERROR_URI);
        }

        // 参数
        Map<String, Object> additionalParameters = new HashMap<>();
        parameters.forEach((key, value) ->
                additionalParameters.put(key, (value.size() == 1) ? value.get(0) : value.toArray(new String[0])));

        // pre_grant_type (REQUIRED)
        String preGrantType = request.getParameter(IngotOAuth2ParameterNames.PRE_GRANT_TYPE);
        if (StrUtil.isEmpty(preGrantType)) {
            OAuth2ErrorUtils.throwInvalidRequestParameter(IngotOAuth2ParameterNames.PRE_GRANT_TYPE, null);
        }

        RegisteredClient client = clientAuthentication.getRegisteredClient();
        return OAuth2PreAuthorizationCodeRequestAuthenticationToken.unauthenticated(
                authentication, preGrantType, client, additionalParameters);
    }
}
