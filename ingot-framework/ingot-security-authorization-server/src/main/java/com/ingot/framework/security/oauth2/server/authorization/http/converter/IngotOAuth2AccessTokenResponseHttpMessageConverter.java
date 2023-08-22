package com.ingot.framework.security.oauth2.server.authorization.http.converter;

import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;

/**
 * <p>Description  : IngotOAuth2AccessTokenResponseHttpMessageConverter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/24.</p>
 * <p>Time         : 3:54 下午.</p>
 */
public class IngotOAuth2AccessTokenResponseHttpMessageConverter
        extends OAuth2AccessTokenResponseHttpMessageConverter {

    public IngotOAuth2AccessTokenResponseHttpMessageConverter() {
        super();
        setAccessTokenResponseParametersConverter(new IngotOAuth2AccessTokenResponseMapConverter());
    }
}
