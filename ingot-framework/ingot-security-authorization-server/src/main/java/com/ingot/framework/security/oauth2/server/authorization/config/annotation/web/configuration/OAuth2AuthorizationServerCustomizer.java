package com.ingot.framework.security.oauth2.server.authorization.config.annotation.web.configuration;

import com.ingot.framework.security.oauth2.server.authorization.web.authentication.DefaultAuthenticationFailureHandler;
import com.ingot.framework.security.oauth2.server.authorization.web.authentication.IngotOAuth2AuthorizationCodeRequestAuthenticationConverter;
import org.springframework.security.config.Customizer;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationEndpointConfigurer;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2AuthorizationCodeRequestAuthenticationConverter;

/**
 * <p>Description  : OAuth2AuthorizationServerCustomizer.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/9.</p>
 * <p>Time         : 10:54 AM.</p>
 */
public class OAuth2AuthorizationServerCustomizer implements Customizer<OAuth2AuthorizationEndpointConfigurer> {

    @Override
    public void customize(OAuth2AuthorizationEndpointConfigurer configurer) {
        // remove OAuth2AuthorizationCodeRequestAuthenticationConverter
        // add IngotOAuth2AuthorizationCodeRequestAuthenticationConverter
        // 替换原有Converter
        configurer.authorizationRequestConverters(converters -> {
            converters.removeIf(converter -> converter instanceof OAuth2AuthorizationCodeRequestAuthenticationConverter);
            converters.add(new IngotOAuth2AuthorizationCodeRequestAuthenticationConverter());
        });
        configurer.errorResponseHandler(new DefaultAuthenticationFailureHandler());
    }
}
