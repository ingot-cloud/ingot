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
public class IngotOAuth2AccessToken implements OAuth2AccessToken{
    private final OAuth2AccessToken accessToken;

    @Override public Map<String, Object> getAdditionalInformation() {
        return accessToken.getAdditionalInformation();
    }

    @Override public Set<String> getScope() {
        return accessToken.getScope();
    }

    @Override public OAuth2RefreshToken getRefreshToken() {
        return accessToken.getRefreshToken();
    }

    @Override public String getTokenType() {
        return accessToken.getTokenType();
    }

    @Override public boolean isExpired() {
        return accessToken.isExpired();
    }

    @Override public Date getExpiration() {
        return accessToken.getExpiration();
    }

    @Override public int getExpiresIn() {
        return accessToken.getExpiresIn();
    }

    @Override public String getValue() {
        return accessToken.getValue();
    }
}
