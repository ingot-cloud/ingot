package com.ingot.framework.security.oauth2.server.authorization.http.converter;

import java.util.HashMap;
import java.util.Map;

import com.ingot.framework.commons.model.support.R;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.util.StringUtils;

/**
 * <p>Description  : 自定义OAuth2Error to Map.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/13.</p>
 * <p>Time         : 10:20 上午.</p>
 */
public class CustomOAuth2ErrorParametersConverter implements Converter<OAuth2Error, Map<String, String>> {

    @Override
    public Map<String, String> convert(OAuth2Error oauth2Error) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put(R.CODE, oauth2Error.getErrorCode());
        if (StringUtils.hasText(oauth2Error.getDescription())) {
            parameters.put(R.MESSAGE, oauth2Error.getDescription());
        }
        if (StringUtils.hasText(oauth2Error.getUri())) {
            parameters.put(R.DATA, oauth2Error.getUri());
        }
        return parameters;
    }
}
