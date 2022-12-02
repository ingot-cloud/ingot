package com.ingot.framework.security.oauth2.server.authorization.http.converter;

import java.util.Map;

import com.ingot.framework.core.model.support.R;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.core.OAuth2Error;

/**
 * <p>Description  : IngotOAuth2ErrorConverter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/13.</p>
 * <p>Time         : 10:20 上午.</p>
 */
public class IngotOAuth2ErrorConverter implements Converter<Map<String, String>, OAuth2Error> {

    @Override
    public OAuth2Error convert(Map<String, String> parameters) {
        String errorCode = parameters.get(R.CODE);
        String errorDescription = parameters.get(R.MESSAGE);
        String errorUri = parameters.get(R.DATA);
        return new OAuth2Error(errorCode, errorDescription, errorUri);
    }
}
