package com.ingot.framework.security.oauth2.core.http.converter;

import com.ingot.framework.core.wrapper.IngotResponse;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Description  : IngotOAuth2ErrorParametersConverter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/13.</p>
 * <p>Time         : 10:20 上午.</p>
 */
public class IngotOAuth2ErrorParametersConverter implements Converter<OAuth2Error, Map<String, String>> {

    @Override
    public Map<String, String> convert(OAuth2Error oauth2Error) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put(IngotResponse.CODE, oauth2Error.getErrorCode());
        if (StringUtils.hasText(oauth2Error.getDescription())) {
            parameters.put(IngotResponse.MESSAGE, oauth2Error.getDescription());
        }
        if (StringUtils.hasText(oauth2Error.getUri())) {
            parameters.put(IngotResponse.DATA, oauth2Error.getUri());
        }
        return parameters;
    }
}
