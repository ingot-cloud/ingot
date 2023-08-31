package com.ingot.framework.security.oauth2.server.authorization.authentication;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.security.core.IngotSecurityMessageSource;
import com.ingot.framework.security.core.tenantdetails.TenantDetails;
import com.ingot.framework.security.core.tenantdetails.TenantDetailsService;
import com.ingot.framework.security.core.userdetails.IngotUser;
import com.ingot.framework.security.oauth2.core.IngotAuthorizationGrantType;
import com.ingot.framework.security.oauth2.core.OAuth2ErrorUtils;
import com.ingot.framework.security.oauth2.core.endpoint.IngotOAuth2ParameterNames;
import com.ingot.framework.security.oauth2.server.authorization.code.OAuth2PreAuthorization;
import com.ingot.framework.security.oauth2.server.authorization.code.OAuth2PreAuthorizationCode;
import com.ingot.framework.security.oauth2.server.authorization.code.OAuth2PreAuthorizationService;
import lombok.Setter;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.util.StringUtils;

import java.security.Principal;

/**
 * <p>Description  : {@link IngotAuthorizationGrantType#PRE_AUTHORIZATION_CODE} request authentication provider.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/26.</p>
 * <p>Time         : 2:51 PM.</p>
 */
public class OAuth2PreAuthorizationCodeRequestAuthenticationProvider implements AuthenticationProvider {
    private final MessageSourceAccessor messages = IngotSecurityMessageSource.getAccessor();
    private OAuth2TokenGenerator<OAuth2PreAuthorizationCode> authorizationCodeGenerator = new OAuth2PreAuthorizationCodeGenerator();
    @Setter
    private TenantDetailsService tenantDetailsService;
    @Setter
    private OAuth2PreAuthorizationService OAuth2PreAuthorizationService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        OAuth2PreAuthorizationCodeRequestAuthenticationToken preAuthorizationAuthenticationToken =
                (OAuth2PreAuthorizationCodeRequestAuthenticationToken) authentication;

        RegisteredClient registeredClient = preAuthorizationAuthenticationToken.getRegisteredClient();
        if (!registeredClient.getAuthorizationGrantTypes().contains(IngotAuthorizationGrantType.PRE_AUTHORIZATION_CODE)) {
            OAuth2ErrorUtils.throwAuthenticationException(
                    OAuth2ErrorCodes.UNAUTHORIZED_CLIENT, this.messages
                            .getMessage("OAuth2PreAuthorizationAuthenticationProvider.unauthorizedClient",
                                    "客户端未授权"));
        }

        // code_challenge (REQUIRED for public clients) - RFC 7636 (PKCE)
        String codeChallenge = (String) preAuthorizationAuthenticationToken.getAdditionalParameters().get(PkceParameterNames.CODE_CHALLENGE);
        if (StrUtil.isNotEmpty(codeChallenge)) {
            String codeChallengeMethod = (String) preAuthorizationAuthenticationToken.getAdditionalParameters().get(PkceParameterNames.CODE_CHALLENGE_METHOD);
            if (!StringUtils.hasText(codeChallengeMethod) || !"S256".equals(codeChallengeMethod)) {
                OAuth2ErrorUtils.throwInvalidRequestParameter(OAuth2ErrorCodes.INVALID_REQUEST, PkceParameterNames.CODE_CHALLENGE_METHOD);
            }
        } else if (registeredClient.getClientSettings().isRequireProofKey()) {
            OAuth2ErrorUtils.throwInvalidRequestParameter(OAuth2ErrorCodes.INVALID_REQUEST, PkceParameterNames.CODE_CHALLENGE);
        }

        // 1.获取用户信息
        Authentication userAuth = (Authentication) preAuthorizationAuthenticationToken.getPrincipal();
        IngotUser user = null;
        if (userAuth.getPrincipal() instanceof IngotUser) {
            user = (IngotUser) userAuth.getPrincipal();
        }
        if (user == null) {
            OAuth2ErrorUtils.throwNotAllowClient(this.messages
                    .getMessage("OAuth2PreAuthorizationAuthenticationProvider.userCantAccess",
                            "用户无法访问"));
        }

        // 2.通过用户信息获取可以访问的租户列表
        TenantDetails tenant = this.tenantDetailsService.loadByUsername(user.getUsername());

        // 3.生成code
        OAuth2TokenContext tokenContext = createAuthorizationCodeTokenContext(
                preAuthorizationAuthenticationToken, registeredClient);
        OAuth2PreAuthorizationCode authorizationCode = this.authorizationCodeGenerator.generate(tokenContext);

        OAuth2PreAuthorization authorization = OAuth2PreAuthorization.withRegisteredClient(registeredClient)
                .principalName(user.getUsername())
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .attribute(Principal.class.getName(), userAuth)
                .token(authorizationCode)
                .build();

        this.OAuth2PreAuthorizationService.save(authorization);

        return OAuth2PreAuthorizationCodeRequestAuthenticationToken
                .authenticated(authorization.getToken().getTokenValue(), tenant.getAllow());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return (OAuth2PreAuthorizationCodeRequestAuthenticationToken.class.isAssignableFrom(authentication));
    }

    private static OAuth2TokenContext createAuthorizationCodeTokenContext(
            OAuth2PreAuthorizationCodeRequestAuthenticationToken preAuthorizationAuthenticationToken,
            RegisteredClient registeredClient) {

        // @formatter:off
        DefaultOAuth2TokenContext.Builder tokenContextBuilder = DefaultOAuth2TokenContext.builder()
                .registeredClient(registeredClient)
                .principal((Authentication) preAuthorizationAuthenticationToken.getPrincipal())
                .tokenType(new OAuth2TokenType(IngotOAuth2ParameterNames.PRE_CODE))
                .authorizationGrantType(IngotAuthorizationGrantType.PRE_AUTHORIZATION_CODE)
                .authorizationGrant(preAuthorizationAuthenticationToken);
        // @formatter:on

        return tokenContextBuilder.build();
    }

    public void setAuthorizationCodeGenerator(OAuth2TokenGenerator<OAuth2PreAuthorizationCode> authorizationCodeGenerator) {
        this.authorizationCodeGenerator = authorizationCodeGenerator;
    }
}
