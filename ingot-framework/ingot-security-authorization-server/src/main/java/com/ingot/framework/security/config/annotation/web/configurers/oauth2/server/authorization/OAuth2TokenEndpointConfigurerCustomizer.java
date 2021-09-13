package com.ingot.framework.security.config.annotation.web.configurers.oauth2.server.authorization;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2TokenEndpointConfigurer;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.server.authorization.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientCredentialsAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2RefreshTokenAuthenticationProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Description  : OAuth2TokenEndpointConfigurerCustomizer.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/9.</p>
 * <p>Time         : 10:06 上午.</p>
 */
@Slf4j
@AllArgsConstructor
public class OAuth2TokenEndpointConfigurerCustomizer implements Customizer<OAuth2TokenEndpointConfigurer> {
    private final HttpSecurity http;

    @Override
    public void customize(OAuth2TokenEndpointConfigurer oAuth2TokenEndpointConfigurer) {
        // todo 无法在 customize 中调用 http.getSharedObject()，获取的数据都为 null
//        OAuth2AuthorizationService authorizationService = http.getSharedObject(OAuth2AuthorizationService.class);
//        JwtEncoder jwtEncoder = http.getSharedObject(JwtEncoder.class);
//
//        log.info("--- auth service={}, jwtEncoder={}", authorizationService, jwtEncoder);
//        oAuth2TokenEndpointConfigurer.authenticationProvider(
//                new OAuth2PasswordAuthenticationProvider(authorizationService, jwtEncoder));
//
//        createDefaultAuthenticationProviders(http)
//                .forEach(oAuth2TokenEndpointConfigurer::authenticationProvider);
    }

    /**
     * {@link OAuth2TokenEndpointConfigurer} createDefaultAuthenticationProviders
     */
    private <B extends HttpSecurityBuilder<B>> List<AuthenticationProvider> createDefaultAuthenticationProviders(B builder) {
        List<AuthenticationProvider> authenticationProviders = new ArrayList<>();

        JwtEncoder jwtEncoder = OAuth2ConfigurerUtils.getJwtEncoder(builder);
        OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer = OAuth2ConfigurerUtils.getJwtCustomizer(builder);

        OAuth2AuthorizationCodeAuthenticationProvider authorizationCodeAuthenticationProvider =
                new OAuth2AuthorizationCodeAuthenticationProvider(
                        OAuth2ConfigurerUtils.getAuthorizationService(builder),
                        jwtEncoder);
        if (jwtCustomizer != null) {
            authorizationCodeAuthenticationProvider.setJwtCustomizer(jwtCustomizer);
        }
        authenticationProviders.add(authorizationCodeAuthenticationProvider);

        OAuth2RefreshTokenAuthenticationProvider refreshTokenAuthenticationProvider =
                new OAuth2RefreshTokenAuthenticationProvider(
                        OAuth2ConfigurerUtils.getAuthorizationService(builder),
                        jwtEncoder);
        if (jwtCustomizer != null) {
            refreshTokenAuthenticationProvider.setJwtCustomizer(jwtCustomizer);
        }
        authenticationProviders.add(refreshTokenAuthenticationProvider);

        OAuth2ClientCredentialsAuthenticationProvider clientCredentialsAuthenticationProvider =
                new OAuth2ClientCredentialsAuthenticationProvider(
                        OAuth2ConfigurerUtils.getAuthorizationService(builder),
                        jwtEncoder);
        if (jwtCustomizer != null) {
            clientCredentialsAuthenticationProvider.setJwtCustomizer(jwtCustomizer);
        }
        authenticationProviders.add(clientCredentialsAuthenticationProvider);

        return authenticationProviders;
    }
}
