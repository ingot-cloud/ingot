package com.ingot.framework.security.oauth2.server.authorization.web.authentication;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.security.oauth2.core.InClientAuthenticationMethod;
import com.ingot.framework.security.oauth2.core.OAuth2ErrorUtils;
import com.ingot.framework.commons.constants.InOAuth2ParameterNames;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Description  : PreAuthClientAuthenticationConverter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/5.</p>
 * <p>Time         : 9:55 PM.</p>
 */
public class PreAuthClientAuthenticationConverter implements AuthenticationConverter {
    private static final String PKCE_ERROR_URI = "https://datatracker.ietf.org/doc/html/rfc7636#section-4.4.1";

    @Nullable
    @Override
    public Authentication convert(HttpServletRequest request) {
        // Must post
        if (!"POST".equals(request.getMethod())) {
            return null;
        }

        // 如果持有已经认证的OAuth2PreAuthorizationCodeRequestAuthenticationToken, 那么不进行client认证
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (OAuth2PreAuthorizationUtils.hadOAuth2PreAuthorizationCodeRequestAuthenticationToken(authentication, request)) {
            return null;
        }

        MultiValueMap<String, String> parameters = OAuth2EndpointUtils.getParameters(request);

        // pre_grant_type (REQUIRED)
        String preGrantType = request.getParameter(InOAuth2ParameterNames.PRE_GRANT_TYPE);
        if (StrUtil.isEmpty(preGrantType)) {
            OAuth2ErrorUtils.throwInvalidRequestParameter(InOAuth2ParameterNames.PRE_GRANT_TYPE, null);
        }

        // client_id (REQUIRED for public clients)
        String clientId = parameters.getFirst(OAuth2ParameterNames.CLIENT_ID);
        if (StrUtil.isEmpty(clientId) ||
                parameters.get(OAuth2ParameterNames.CLIENT_ID).size() != 1) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_REQUEST);
        }

        // code_challenge (REQUIRED for public clients) - RFC 7636 (PKCE)
        String codeChallenge = parameters.getFirst(PkceParameterNames.CODE_CHALLENGE);
        if (StrUtil.isEmpty(codeChallenge) ||
                parameters.get(PkceParameterNames.CODE_CHALLENGE).size() != 1) {
            OAuth2ErrorUtils.throwInvalidRequestParameter(PkceParameterNames.CODE_CHALLENGE, PKCE_ERROR_URI);
        }

        parameters.remove(OAuth2ParameterNames.CLIENT_ID);

        Map<String, Object> additionalParameters = new HashMap<>();
        parameters.forEach((key, value) ->
                additionalParameters.put(key, (value.size() == 1) ? value.get(0) : value.toArray(new String[0])));

        return new OAuth2ClientAuthenticationToken(clientId, InClientAuthenticationMethod.PRE_AUTH, null,
                additionalParameters);
    }
}
