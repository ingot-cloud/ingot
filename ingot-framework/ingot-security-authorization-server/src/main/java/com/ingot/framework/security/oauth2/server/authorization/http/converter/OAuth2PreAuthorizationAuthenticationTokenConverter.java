package com.ingot.framework.security.oauth2.server.authorization.http.converter;

import com.ingot.framework.core.model.status.BaseErrorCode;
import com.ingot.framework.core.model.support.R;
import com.ingot.framework.security.oauth2.core.endpoint.IngotOAuth2ParameterNames;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2PreAuthorizationCodeRequestAuthenticationToken;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Description  : OAuth2PreAuthorizationAuthenticationTokenConverter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/28.</p>
 * <p>Time         : 5:28 PM.</p>
 */
public class OAuth2PreAuthorizationAuthenticationTokenConverter implements Converter<OAuth2PreAuthorizationCodeRequestAuthenticationToken, Map<String, Object>> {

    @Override
    public Map<String, Object> convert(@NonNull OAuth2PreAuthorizationCodeRequestAuthenticationToken source) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(IngotOAuth2ParameterNames.PRE_ALLOW_LIST, source.getAllowList());

        Map<String, Object> result = new HashMap<>();
        result.put(R.CODE, BaseErrorCode.OK.getCode());
        result.put(R.DATA, parameters);
        return result;
    }
}
