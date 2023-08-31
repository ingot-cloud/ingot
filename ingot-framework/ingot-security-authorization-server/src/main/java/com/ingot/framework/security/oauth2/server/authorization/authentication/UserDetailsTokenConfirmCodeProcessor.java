package com.ingot.framework.security.oauth2.server.authorization.authentication;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.security.core.userdetails.UserDetailsAuthorizationGrantType;
import com.ingot.framework.security.oauth2.core.OAuth2ErrorUtils;
import com.ingot.framework.security.oauth2.server.authorization.code.OAuth2PreAuthorization;
import com.ingot.framework.security.oauth2.server.authorization.code.PreAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import static com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2AuthenticationProviderUtils.getAuthenticatedClientElseThrowInvalidClient;

/**
 * <p>Description  : UserDetailsTokenConfirmCodeProcessor.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/8/31.</p>
 * <p>Time         : 9:36 AM.</p>
 */
@RequiredArgsConstructor
public class UserDetailsTokenConfirmCodeProcessor implements UserDetailsTokenProcessor {
    private final PreAuthorizationService preAuthorizationService;
    private final MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();

    @Override
    public OAuth2UserDetailsAuthenticationToken process(OAuth2UserDetailsAuthenticationToken in) {
        // confirm_code 模式，需要转换token，设置username
        if (in.getGrantType() != UserDetailsAuthorizationGrantType.CONFIRM_CODE) {
            return null;
        }

        OAuth2ClientAuthenticationToken clientPrincipal =
                getAuthenticatedClientElseThrowInvalidClient(in);
        RegisteredClient registeredClient = clientPrincipal.getRegisteredClient();

        if (registeredClient == null ||
                !registeredClient.getAuthorizationGrantTypes().contains(in.getGrantType())) {
            OAuth2ErrorUtils.throwAuthenticationException(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT);
        }

        OAuth2PreAuthorization authorization = preAuthorizationService.get(in.getPrincipal().toString());
        if (authorization == null) {
            OAuth2ErrorUtils.throwPreAuthorizationCodeExpired(this.messages
                    .getMessage("OAuth2UserDetailsAuthenticationProvider.preAuthorizationCodeExpired",
                            "登录超时"));
        }
        // 判断是否登录的同一个client
        if (!StrUtil.equals(authorization.getRegisteredClientId(), registeredClient.getClientId())) {
            OAuth2ErrorUtils.throwNotAllowClient(this.messages
                    .getMessage("OAuth2UserDetailsAuthenticationProvider.notAllowClient",
                            "不允许访问客户端"));
        }

        return OAuth2UserDetailsAuthenticationToken.unauthenticated(
                authorization.getPrincipalName(), null,
                in.getGrantType(), in.getClient());
    }
}
