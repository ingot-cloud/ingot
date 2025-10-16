package com.ingot.framework.security.oauth2.server.authorization.config.annotation.web.configuration;

import com.ingot.framework.security.oauth2.server.authorization.web.authentication.AccessTokenAuthenticationSuccessHandler;
import com.ingot.framework.security.oauth2.server.authorization.web.authentication.DefaultAuthenticationFailureHandler;
import com.ingot.framework.security.oauth2.server.authorization.web.authentication.OAuth2CustomAuthenticationConverter;
import org.springframework.security.config.Customizer;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2TokenEndpointConfigurer;

/**
 * <p>Description  : OAuth2TokenEndpointCustomizer.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/9.</p>
 * <p>Time         : 10:47 AM.</p>
 */
public class OAuth2TokenEndpointCustomizer implements Customizer<OAuth2TokenEndpointConfigurer> {

    @Override
    public void customize(OAuth2TokenEndpointConfigurer configurer) {
        // todo AccessTokenAuthenticationSuccessHandler 区分grant type，进行不同login event处理
        // 1. 添加 OAuth2CustomAuthenticationConverter
        configurer.accessTokenRequestConverter(new OAuth2CustomAuthenticationConverter());
        // 2. 自定义 AuthenticationSuccessHandler 和 AuthenticationFailureHandler
        configurer.accessTokenResponseHandler(new AccessTokenAuthenticationSuccessHandler())
                .errorResponseHandler(new DefaultAuthenticationFailureHandler());
    }
}
