package com.ingot.framework.security.oauth2.server.authorization.authentication;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import com.ingot.framework.core.model.common.AllowTenantDTO;
import com.ingot.framework.security.core.IngotSecurityMessageSource;
import com.ingot.framework.security.core.authority.IngotAuthorityUtils;
import com.ingot.framework.security.core.userdetails.IngotUser;
import com.ingot.framework.security.oauth2.core.IngotAuthorizationGrantType;
import com.ingot.framework.security.oauth2.core.OAuth2ErrorUtils;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.util.List;
import java.util.Map;

/**
 * <p>Description  : {@link IngotAuthorizationGrantType#PRE_AUTHORIZATION_CODE} request authentication provider.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/26.</p>
 * <p>Time         : 2:51 PM.</p>
 */
public class OAuth2PreAuthorizationCodeRequestAuthenticationProvider implements AuthenticationProvider {
    private final MessageSourceAccessor messages = IngotSecurityMessageSource.getAccessor();

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

        Map<String, Object> additionalParameters = preAuthorizationAuthenticationToken.getAdditionalParameters();

        // check redirect_uri
        String redirectUri = (String) additionalParameters.get(OAuth2ParameterNames.REDIRECT_URI);
        if (!registeredClient.getRedirectUris().contains(redirectUri)) {
            OAuth2ErrorUtils.throwInvalidRequestParameter(OAuth2ParameterNames.REDIRECT_URI, null);
        }

        // code_challenge (REQUIRED for public clients) - RFC 7636 (PKCE)
        String codeChallenge = (String) additionalParameters.get(PkceParameterNames.CODE_CHALLENGE);
        if (StrUtil.isEmpty(codeChallenge) && registeredClient.getClientSettings().isRequireProofKey()) {
            OAuth2ErrorUtils.throwInvalidRequestParameter(PkceParameterNames.CODE_CHALLENGE, null);
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

        // 保存会话时长，使用刷新token持续时间
        long timeToLive = registeredClient.getTokenSettings().getRefreshTokenTimeToLive().getSeconds();
        List<AllowTenantDTO> allows = ListUtil.list(false, IngotAuthorityUtils.extractAllowTenants(user.getAuthorities()));
        return OAuth2PreAuthorizationCodeRequestAuthenticationToken
                .authenticated(user, allows, additionalParameters, timeToLive);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return (OAuth2PreAuthorizationCodeRequestAuthenticationToken.class.isAssignableFrom(authentication));
    }
}
