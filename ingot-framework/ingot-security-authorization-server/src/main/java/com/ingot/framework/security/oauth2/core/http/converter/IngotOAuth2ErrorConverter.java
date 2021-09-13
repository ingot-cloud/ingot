package com.ingot.framework.security.oauth2.core.http.converter;

import com.ingot.framework.core.wrapper.IngotResponse;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.core.OAuth2Error;

import java.util.Map;

/**
 * <p>Description  : IngotOAuth2ErrorConverter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/13.</p>
 * <p>Time         : 10:20 上午.</p>
 */
public class IngotOAuth2ErrorConverter implements Converter<Map<String, String>, OAuth2Error> {

    @Override
    public OAuth2Error convert(Map<String, String> parameters) {
        String errorCode = parameters.get(IngotResponse.CODE);
        String errorDescription = parameters.get(IngotResponse.MESSAGE);
        String errorUri = parameters.get(IngotResponse.DATA);
        return new OAuth2Error(errorCode, errorDescription, errorUri);
    }
}
