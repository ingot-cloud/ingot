package com.ingot.framework.security.oauth2.server.authorization.web.authentication;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.ingot.framework.security.oauth2.core.endpoint.IngotOAuth2ParameterNames;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2PreAuthorizationCodeRequestAuthenticationToken;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationException;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2AuthorizationCodeRequestAuthenticationConverter;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>Description  : IngotOAuth2AuthorizationCodeRequestAuthenticationConverter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/9.</p>
 * <p>Time         : 10:58 AM.</p>
 */
public class IngotOAuth2AuthorizationCodeRequestAuthenticationConverter implements AuthenticationConverter {
    private final OAuth2AuthorizationCodeRequestAuthenticationConverter converter =
            new OAuth2AuthorizationCodeRequestAuthenticationConverter();

    @Override
    public Authentication convert(HttpServletRequest request) {
        Authentication principal = SecurityContextHolder.getContext().getAuthentication();
        if (principal instanceof OAuth2PreAuthorizationCodeRequestAuthenticationToken token) {
            principal = requiredCheck(request, token);
        }

        OAuth2AuthorizationCodeRequestAuthenticationToken token =
                (OAuth2AuthorizationCodeRequestAuthenticationToken) converter.convert(request);

        Map<String, Object> additionalParameters = new HashMap<>(token.getAdditionalParameters());
        // 不用传递method，默认使用S256
        additionalParameters.put(PkceParameterNames.CODE_CHALLENGE_METHOD, "S256");

        return new OAuth2AuthorizationCodeRequestAuthenticationToken(
                token.getAuthorizationUri(), token.getClientId(), principal,
                token.getRedirectUri(), token.getState(), token.getScopes(), additionalParameters);
    }

    private Authentication requiredCheck(HttpServletRequest request, OAuth2PreAuthorizationCodeRequestAuthenticationToken token) {
        // tenant (REQUIRED)
        String tenant = request.getParameter(IngotOAuth2ParameterNames.TENANT);
        if (StrUtil.isEmpty(tenant)) {
            throwError(IngotOAuth2ParameterNames.TENANT);
        }

        // tenant必须在allow list中
        if (token.getAllowList().stream().noneMatch(item -> StrUtil.equals(item.getId(), tenant))) {
            throwError(IngotOAuth2ParameterNames.TENANT);
        }

        MultiValueMap<String, String> parameters = OAuth2EndpointUtils.getParameters(request);
        Map<String, Object> additionalParameters = token.getAdditionalParameters();
        additionalParameters.forEach((key, value) -> {
            List<String> requestValues = parameters.get(key);
            if (CollUtil.isEmpty(requestValues)) {
                throwError(key);
            }
            if (value.getClass().isArray() && !ArrayUtil.equals(ArrayUtil.toArray(requestValues, String.class), value)) {
                throwError(key);
            }
            if (!StrUtil.equals(requestValues.get(0), String.valueOf(value))) {
                throwError(key);
            }
        });

        Map<String, Object> newAdditionalParameters = new HashMap<>(additionalParameters);
        newAdditionalParameters.put(IngotOAuth2ParameterNames.TENANT, tenant);
        return OAuth2PreAuthorizationCodeRequestAuthenticationToken.authenticated(
                token.getPrincipal(), token.getAllowList(), newAdditionalParameters, token.getTimeToLive());
    }

    private static void throwError(String parameterName) {
        OAuth2Error error = new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST, "OAuth 2.0 Parameter: " + parameterName, null);
        throw new OAuth2AuthorizationCodeRequestAuthenticationException(error, null);
    }
}
