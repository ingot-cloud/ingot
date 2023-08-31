package com.ingot.framework.security.oauth2.server.authorization.authentication;

import com.ingot.framework.security.oauth2.core.endpoint.IngotOAuth2ParameterNames;
import com.ingot.framework.security.oauth2.server.authorization.code.OAuth2PreAuthorizationCode;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

import java.time.Instant;
import java.util.Base64;

/**
 * <p>Description  : OAuth2PreAuthorizationCodeGenerator.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/8/31.</p>
 * <p>Time         : 10:03 AM.</p>
 */
final class OAuth2PreAuthorizationCodeGenerator implements OAuth2TokenGenerator<OAuth2PreAuthorizationCode> {
    private final StringKeyGenerator authorizationCodeGenerator =
            new Base64StringKeyGenerator(Base64.getUrlEncoder().withoutPadding(), 96);

    @Override
    public OAuth2PreAuthorizationCode generate(OAuth2TokenContext context) {
        if (context.getTokenType() == null ||
                !IngotOAuth2ParameterNames.PRE_CODE.equals(context.getTokenType().getValue())) {
            return null;
        }
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(context.getRegisteredClient().getTokenSettings().getAuthorizationCodeTimeToLive());
        return new OAuth2PreAuthorizationCode(this.authorizationCodeGenerator.generateKey(), issuedAt, expiresAt);
    }
}
