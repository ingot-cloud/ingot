package com.ingot.framework.security.provider.token;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.ingot.framework.security.provider.IngotOAuth2AccessTokenSerializer;
import lombok.AllArgsConstructor;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;

import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * <p>Description  : IngotOAuth2AccessToken.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/26.</p>
 * <p>Time         : 5:17 下午.</p>
 */
@AllArgsConstructor
@JsonSerialize(using = IngotOAuth2AccessTokenSerializer.class)
public class IngotOAuth2AccessToken implements OAuth2AccessToken {
    /**
     * The access token issued by the authorization server. This value is REQUIRED.
     */
    public static String ACCESS_TOKEN = "accessToken";

    /**
     * The type of the token issued as described in <a
     * href="http://tools.ietf.org/html/draft-ietf-oauth-v2-22#section-7.1">Section 7.1</a>. Value is case insensitive.
     * This value is REQUIRED.
     */
    public static String TOKEN_TYPE = "tokenType";

    /**
     * The lifetime in seconds of the access token. For example, the value "3600" denotes that the access token will
     * expire in one hour from the time the response was generated. This value is OPTIONAL.
     */
    public static String EXPIRES_IN = "expiresIn";

    /**
     * The refresh token which can be used to obtain new access tokens using the same authorization grant as described
     * in <a href="http://tools.ietf.org/html/draft-ietf-oauth-v2-22#section-6">Section 6</a>. This value is OPTIONAL.
     */
    public static String REFRESH_TOKEN = "refreshToken";

    /**
     * The scope of the access token as described by <a
     * href="http://tools.ietf.org/html/draft-ietf-oauth-v2-22#section-3.3">Section 3.3</a>
     */
    public static String SCOPE = "scope";


    private final OAuth2AccessToken accessToken;

    @Override
    public Map<String, Object> getAdditionalInformation() {
        return accessToken.getAdditionalInformation();
    }

    @Override
    public Set<String> getScope() {
        return accessToken.getScope();
    }

    @Override
    public OAuth2RefreshToken getRefreshToken() {
        return accessToken.getRefreshToken();
    }

    @Override
    public String getTokenType() {
        return accessToken.getTokenType();
    }

    @Override
    public boolean isExpired() {
        return accessToken.isExpired();
    }

    @Override
    public Date getExpiration() {
        return accessToken.getExpiration();
    }

    @Override
    public int getExpiresIn() {
        return accessToken.getExpiresIn();
    }

    @Override
    public String getValue() {
        return accessToken.getValue();
    }
}
