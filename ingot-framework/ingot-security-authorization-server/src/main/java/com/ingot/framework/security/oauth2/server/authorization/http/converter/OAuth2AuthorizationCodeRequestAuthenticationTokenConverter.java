package com.ingot.framework.security.oauth2.server.authorization.http.converter;

import com.ingot.framework.core.model.status.BaseErrorCode;
import com.ingot.framework.core.model.support.R;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Description  : OAuth2AuthorizationCodeRequestAuthenticationTokenConverter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/9.</p>
 * <p>Time         : 4:12 PM.</p>
 */
public class OAuth2AuthorizationCodeRequestAuthenticationTokenConverter implements Converter<OAuth2AuthorizationCodeRequestAuthenticationToken, Map<String, Object>> {

    @Override
    public Map<String, Object> convert(OAuth2AuthorizationCodeRequestAuthenticationToken source) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(OAuth2ParameterNames.CODE, source.getAuthorizationCode().getTokenValue());
        parameters.put(OAuth2ParameterNames.REDIRECT_URI, source.getRedirectUri());
        parameters.put(OAuth2ParameterNames.STATE, source.getState());

        Map<String, Object> result = new HashMap<>();
        result.put(R.CODE, BaseErrorCode.OK.getCode());
        result.put(R.DATA, parameters);
        return result;
    }
}
