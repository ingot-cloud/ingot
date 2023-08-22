package com.ingot.framework.security.config.annotation.web.configuration;

import com.ingot.framework.security.oauth2.core.PermitResolver;
import com.ingot.framework.security.oauth2.server.resource.authentication.IngotJwtAuthenticationConverter;
import com.ingot.framework.security.oauth2.server.resource.web.IngotBearerTokenAuthenticationEntryPoint;
import com.ingot.framework.security.oauth2.server.resource.web.IngotBearerTokenResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;

/**
 * <p>Description  : OAuth2ResourceServerCustomizer.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/11/19.</p>
 * <p>Time         : 1:46 PM.</p>
 */
@RequiredArgsConstructor
public class OAuth2ResourceServerCustomizer implements Customizer<OAuth2ResourceServerConfigurer<HttpSecurity>> {
    private final PermitResolver permitResolver;

    @Override
    public void customize(OAuth2ResourceServerConfigurer<HttpSecurity> configurer) {
        configurer.authenticationEntryPoint(new IngotBearerTokenAuthenticationEntryPoint())
                .bearerTokenResolver(new IngotBearerTokenResolver(permitResolver))
                .jwt(jwtConfigurer -> jwtConfigurer.jwtAuthenticationConverter(new IngotJwtAuthenticationConverter()));
    }
}
