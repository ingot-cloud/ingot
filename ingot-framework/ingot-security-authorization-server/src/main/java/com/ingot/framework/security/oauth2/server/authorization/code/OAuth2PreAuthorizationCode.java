package com.ingot.framework.security.oauth2.server.authorization.code;

import org.springframework.security.oauth2.core.AbstractOAuth2Token;

import java.time.Instant;

/**
 * <p>Description  : OAuth2PreAuthorizationCode.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/8/31.</p>
 * <p>Time         : 10:07 AM.</p>
 */
public class OAuth2PreAuthorizationCode extends AbstractOAuth2Token {
    public OAuth2PreAuthorizationCode(String tokenValue, Instant issuedAt, Instant expiresAt) {
        super(tokenValue, issuedAt, expiresAt);
    }
}
