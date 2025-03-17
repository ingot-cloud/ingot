package com.ingot.framework.security.oauth2.server.authorization.http.converter;

import com.ingot.framework.core.model.support.R;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.core.OAuth2Error;

import java.util.Map;

/**
 * <p>Description  : 自定义Map to OAuth2Error.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/13.</p>
 * <p>Time         : 10:20 上午.</p>
 */
public class CustomOAuth2ErrorConverter implements Converter<Map<String, String>, OAuth2Error> {

    @Override
    public OAuth2Error convert(Map<String, String> parameters) {
        String errorCode = parameters.get(R.CODE);
        String errorDescription = parameters.get(R.MESSAGE);
        String errorUri = parameters.get(R.DATA);
        return new OAuth2Error(errorCode, errorDescription, errorUri);
    }
}
