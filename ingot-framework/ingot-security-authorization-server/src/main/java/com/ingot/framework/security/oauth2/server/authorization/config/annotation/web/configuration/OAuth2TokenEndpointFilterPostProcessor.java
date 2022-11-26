package com.ingot.framework.security.oauth2.server.authorization.config.annotation.web.configuration;

import java.util.Arrays;

import com.ingot.framework.security.oauth2.server.authorization.web.authentication.AccessTokenAuthenticationFailureHandler;
import com.ingot.framework.security.oauth2.server.authorization.web.authentication.AccessTokenAuthenticationSuccessHandler;
import com.ingot.framework.security.oauth2.server.authorization.web.authentication.OAuth2PasswordAuthenticationConverter;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.oauth2.server.authorization.web.OAuth2TokenEndpointFilter;
import org.springframework.security.oauth2.server.authorization.web.authentication.DelegatingAuthenticationConverter;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2AuthorizationCodeAuthenticationConverter;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2ClientCredentialsAuthenticationConverter;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2RefreshTokenAuthenticationConverter;

/**
 * <p>Description  : OAuth2TokenEndpointFilterPostProcessor.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/10/15.</p>
 * <p>Time         : 10:20 上午.</p>
 */
public class OAuth2TokenEndpointFilterPostProcessor implements ObjectPostProcessor<OAuth2TokenEndpointFilter> {

    @Override
    public <O extends OAuth2TokenEndpointFilter> O postProcess(O filter) {
        filter.setAuthenticationConverter(
                new DelegatingAuthenticationConverter(
                        Arrays.asList(
                                new OAuth2AuthorizationCodeAuthenticationConverter(),
                                new OAuth2RefreshTokenAuthenticationConverter(),
                                new OAuth2ClientCredentialsAuthenticationConverter(),
                                new OAuth2PasswordAuthenticationConverter())
                )
        );

        filter.setAuthenticationSuccessHandler(
                new AccessTokenAuthenticationSuccessHandler());
        filter.setAuthenticationFailureHandler(
                new AccessTokenAuthenticationFailureHandler());
        return filter;
    }
}
