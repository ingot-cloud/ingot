package com.ingot.framework.security.oauth2.server.authorization.authentication;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import com.ingot.framework.commons.constants.InOAuth2ParameterNames;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.util.Assert;

/**
 * <p>Description  : 自定义授权码{@link AuthenticationProvider}.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/22.</p>
 * <p>Time         : 8:53 AM.</p>
 */
@Slf4j
public final class InOAuth2AuthorizationCodeAuthenticationProvider implements AuthenticationProvider {
    public static final OAuth2TokenType AUTHORIZATION_CODE_TOKEN_TYPE =
            new OAuth2TokenType(OAuth2ParameterNames.CODE);
    private final OAuth2AuthorizationService authorizationService;
    private final OAuth2AuthorizationCodeAuthenticationProvider provider;

    public InOAuth2AuthorizationCodeAuthenticationProvider(OAuth2AuthorizationService authorizationService,
                                                           OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator) {
        Assert.notNull(authorizationService, "authorizationService cannot be null");
        Assert.notNull(tokenGenerator, "tokenGenerator cannot be null");
        this.authorizationService = authorizationService;
        this.provider = new OAuth2AuthorizationCodeAuthenticationProvider(authorizationService, tokenGenerator);
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        OAuth2AuthorizationCodeAuthenticationToken authorizationCodeAuthentication =
                (OAuth2AuthorizationCodeAuthenticationToken) authentication;
        OAuth2Authorization authorization = this.authorizationService.findByToken(
                authorizationCodeAuthentication.getCode(), AUTHORIZATION_CODE_TOKEN_TYPE);
        if (authorization == null) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_GRANT);
        }

        OAuth2AccessTokenAuthenticationToken token = (OAuth2AccessTokenAuthenticationToken) provider.authenticate(authentication);
        Map<String, Object> additionalParameters = token.getAdditionalParameters();
        if (CollUtil.isEmpty(additionalParameters)) {
            additionalParameters = MapUtil.newHashMap();
        }

        // 如果是OAuth2PreAuthorizationCodeRequestAuthenticationToken，那么添加 tenant
        Authentication principal = authorization.getAttribute(Principal.class.getName());
        if (principal instanceof OAuth2PreAuthorizationCodeRequestAuthenticationToken preToken) {
            preToken.getAdditionalParameters().get(InOAuth2ParameterNames.TENANT);
            additionalParameters.put(InOAuth2ParameterNames.TENANT,
                    preToken.getAdditionalParameters().get(InOAuth2ParameterNames.TENANT));
        }
        OAuth2AuthorizationRequest request = authorization.getAttribute(OAuth2AuthorizationRequest.class.getName());
        if (request != null) {
            String sessionId = Optional.ofNullable(request.getAdditionalParameters().get(InOAuth2ParameterNames.SESSION_ID))
                    .map(String::valueOf)
                    .orElse("");
            additionalParameters.put(InOAuth2ParameterNames.SESSION_ID, sessionId);
        }

        return new OAuth2AccessTokenAuthenticationToken(
                token.getRegisteredClient(),
                (Authentication) token.getPrincipal(),
                token.getAccessToken(),
                token.getRefreshToken(), additionalParameters);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return OAuth2AuthorizationCodeAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
