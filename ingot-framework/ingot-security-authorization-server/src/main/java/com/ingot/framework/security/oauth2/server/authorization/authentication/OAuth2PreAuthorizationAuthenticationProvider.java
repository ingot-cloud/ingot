package com.ingot.framework.security.oauth2.server.authorization.authentication;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.ingot.framework.security.core.IngotSecurityMessageSource;
import com.ingot.framework.security.core.tenantdetails.TenantDetails;
import com.ingot.framework.security.core.tenantdetails.TenantDetailsService;
import com.ingot.framework.security.core.userdetails.IngotUser;
import com.ingot.framework.security.core.userdetails.UserDetailsAuthorizationGrantType;
import com.ingot.framework.security.oauth2.core.OAuth2ErrorUtils;
import com.ingot.framework.security.oauth2.server.authorization.code.PreAuthorization;
import com.ingot.framework.security.oauth2.server.authorization.code.PreAuthorizationCodeService;
import lombok.Setter;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.util.StringUtils;

/**
 * <p>Description  : OAuth2PreAuthorizationAuthenticationProvider.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/26.</p>
 * <p>Time         : 2:51 PM.</p>
 */
public class OAuth2PreAuthorizationAuthenticationProvider implements AuthenticationProvider {
    private final MessageSourceAccessor messages = IngotSecurityMessageSource.getAccessor();
    @Setter
    private TenantDetailsService tenantDetailsService;
    @Setter
    private PreAuthorizationCodeService preAuthorizationCodeService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        OAuth2PreAuthorizationAuthenticationToken preAuthorizationAuthenticationToken =
                (OAuth2PreAuthorizationAuthenticationToken) authentication;

        RegisteredClient registeredClient = preAuthorizationAuthenticationToken.getRegisteredClient();
        if (!registeredClient.getAuthorizationGrantTypes().contains(UserDetailsAuthorizationGrantType.CONFIRM_CODE)) {
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
        TenantDetails tenant = tenantDetailsService.loadByUsername(user.getUsername());

        // 3.生成code
        String code = UUID.randomUUID().toString().replace("-", "");
        preAuthorizationCodeService.save(PreAuthorization.create(user), code);
        return OAuth2PreAuthorizationAuthenticationToken.authenticated(code, tenant.getAllow());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return (OAuth2PreAuthorizationAuthenticationToken.class.isAssignableFrom(authentication));
    }
}
