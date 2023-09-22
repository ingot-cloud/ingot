package com.ingot.framework.security.oauth2.server.authorization.http.converter;

import com.ingot.framework.core.model.status.BaseErrorCode;
import com.ingot.framework.core.model.support.R;
import com.ingot.framework.security.oauth2.core.endpoint.IngotOAuth2ParameterNames;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

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
        parameters.put(IngotOAuth2ParameterNames.ACCESS_TOKEN, tokenResponse.getAccessToken().getTokenValue());
        parameters.put(IngotOAuth2ParameterNames.TOKEN_TYPE, tokenResponse.getAccessToken().getTokenType().getValue());
        parameters.put(IngotOAuth2ParameterNames.EXPIRES_IN, String.valueOf(getExpiresIn(tokenResponse)));
        Object tenant = tokenResponse.getAdditionalParameters().get(IngotOAuth2ParameterNames.TENANT);
        if (tenant != null) {
            parameters.put(IngotOAuth2ParameterNames.TENANT, (String) tenant);
        }
        if (!CollectionUtils.isEmpty(tokenResponse.getAccessToken().getScopes())) {
            parameters.put(OAuth2ParameterNames.SCOPE,
                    StringUtils.collectionToDelimitedString(tokenResponse.getAccessToken().getScopes(), " "));
        }
        if (tokenResponse.getRefreshToken() != null) {
            parameters.put(IngotOAuth2ParameterNames.REFRESH_TOKEN, tokenResponse.getRefreshToken().getTokenValue());
        }
        if (!CollectionUtils.isEmpty(tokenResponse.getAdditionalParameters())) {
            for (Map.Entry<String, Object> entry : tokenResponse.getAdditionalParameters().entrySet()) {
                parameters.put(entry.getKey(), entry.getValue().toString());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put(R.CODE, BaseErrorCode.OK.getCode());
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
