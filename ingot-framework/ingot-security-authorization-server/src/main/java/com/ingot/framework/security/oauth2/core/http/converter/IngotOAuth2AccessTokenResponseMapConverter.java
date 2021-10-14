package com.ingot.framework.security.oauth2.core.http.converter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import com.ingot.framework.common.status.BaseStatusCode;
import com.ingot.framework.core.wrapper.R;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * <p>Description  : IngotOAuth2AccessTokenResponseMapConverter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/24.</p>
 * <p>Time         : 3:58 下午.</p>
 */
public class IngotOAuth2AccessTokenResponseMapConverter
        implements Converter<OAuth2AccessTokenResponse, Map<String, Object>> {

    @Override
    public Map<String, Object> convert(OAuth2AccessTokenResponse tokenResponse) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put(OAuth2ParameterNames.ACCESS_TOKEN, tokenResponse.getAccessToken().getTokenValue());
        parameters.put(OAuth2ParameterNames.TOKEN_TYPE, tokenResponse.getAccessToken().getTokenType().getValue());
        parameters.put(OAuth2ParameterNames.EXPIRES_IN, String.valueOf(getExpiresIn(tokenResponse)));
        if (!CollectionUtils.isEmpty(tokenResponse.getAccessToken().getScopes())) {
            parameters.put(OAuth2ParameterNames.SCOPE,
                    StringUtils.collectionToDelimitedString(tokenResponse.getAccessToken().getScopes(), " "));
        }
        if (tokenResponse.getRefreshToken() != null) {
            parameters.put(OAuth2ParameterNames.REFRESH_TOKEN, tokenResponse.getRefreshToken().getTokenValue());
        }
        if (!CollectionUtils.isEmpty(tokenResponse.getAdditionalParameters())) {
            for (Map.Entry<String, Object> entry : tokenResponse.getAdditionalParameters().entrySet()) {
                parameters.put(entry.getKey(), entry.getValue().toString());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put(R.CODE, BaseStatusCode.OK.code());
        result.put(R.DATA, parameters);
        return result;
    }

    private long getExpiresIn(OAuth2AccessTokenResponse tokenResponse) {
        if (tokenResponse.getAccessToken().getExpiresAt() != null) {
            return ChronoUnit.SECONDS.between(Instant.now(), tokenResponse.getAccessToken().getExpiresAt());
        }
        return -1;
    }
}
