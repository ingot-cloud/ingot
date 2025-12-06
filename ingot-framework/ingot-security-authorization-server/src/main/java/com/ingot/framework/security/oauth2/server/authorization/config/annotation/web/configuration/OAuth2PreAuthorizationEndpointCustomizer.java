package com.ingot.framework.security.oauth2.server.authorization.config.annotation.web.configuration;

import com.ingot.framework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2PreAuthorizationRequestEndpointConfigurer;
import com.ingot.framework.security.oauth2.server.authorization.web.authentication.DefaultAuthenticationFailureHandler;
import org.springframework.security.config.Customizer;

/**
 * <p>Description  : OAuth2PreAuthorizationEndpointCustomizer.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/5.</p>
 * <p>Time         : 10:09.</p>
 */
public class OAuth2PreAuthorizationEndpointCustomizer implements Customizer<OAuth2PreAuthorizationRequestEndpointConfigurer> {

    @Override
    public void customize(OAuth2PreAuthorizationRequestEndpointConfigurer oAuth2PreAuthorizationRequestEndpointConfigurer) {
        oAuth2PreAuthorizationRequestEndpointConfigurer.errorResponseHandler(new DefaultAuthenticationFailureHandler());
    }
}
