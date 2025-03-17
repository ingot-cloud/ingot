package com.ingot.framework.security.oauth2.server.authorization.http.converter;

import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;

/**
 * <p>Description  : 自定义 AccessToken Http Message Converter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/24.</p>
 * <p>Time         : 3:54 下午.</p>
 */
public class CustomOAuth2AccessTokenResponseHttpMessageConverter
        extends OAuth2AccessTokenResponseHttpMessageConverter {

    public CustomOAuth2AccessTokenResponseHttpMessageConverter() {
        super();
        setAccessTokenResponseParametersConverter(new CustomOAuth2AccessTokenResponseMapConverter());
    }
}
