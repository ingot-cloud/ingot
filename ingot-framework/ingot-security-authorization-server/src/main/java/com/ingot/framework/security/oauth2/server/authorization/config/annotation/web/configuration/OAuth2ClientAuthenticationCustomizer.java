package com.ingot.framework.security.oauth2.server.authorization.config.annotation.web.configuration;

import com.ingot.framework.security.oauth2.server.authorization.web.authentication.DefaultAuthenticationFailureHandler;
import org.springframework.security.config.Customizer;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2ClientAuthenticationConfigurer;

/**
 * <p>Description  : OAuth2ClientAuthenticationCustomizer.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/9.</p>
 * <p>Time         : 10:52 AM.</p>
 */
public class OAuth2ClientAuthenticationCustomizer implements Customizer<OAuth2ClientAuthenticationConfigurer> {
    @Override
    public void customize(OAuth2ClientAuthenticationConfigurer configurer) {
        configurer.errorResponseHandler(new DefaultAuthenticationFailureHandler());
    }
}
