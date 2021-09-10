package com.ingot.framework.security.config.annotation.web.configuration;

import com.ingot.framework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2TokenEndpointConfigurerCustomizer;
import com.ingot.framework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2UsernamePasswordAuthenticationConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * <p>Description  : IngotOAuth2AuthorizationServerConfiguration.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/8.</p>
 * <p>Time         : 5:02 下午.</p>
 */
@Configuration(proxyBeanMethods = false)
public class IngotOAuth2AuthorizationServerConfiguration {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        applyDefaultSecurity(http);
        return http.formLogin(Customizer.withDefaults()).build();
    }

    public static void applyDefaultSecurity(HttpSecurity http) throws Exception {
        http.objectPostProcessor(new OAuth2ObjectPostProcessor());

        OAuth2AuthorizationServerConfigurer<HttpSecurity> authorizationServerConfigurer =
                new OAuth2AuthorizationServerConfigurer<>();
        authorizationServerConfigurer.tokenEndpoint(new OAuth2TokenEndpointConfigurerCustomizer(http));
        RequestMatcher endpointsMatcher = authorizationServerConfigurer
                .getEndpointsMatcher();

        OAuth2UsernamePasswordAuthenticationConfigurer<HttpSecurity> usernamePasswordAuthenticationConfigurer =
                new OAuth2UsernamePasswordAuthenticationConfigurer<>();

        http
                .requestMatcher(endpointsMatcher)
                .authorizeRequests(authorizeRequests ->
                        authorizeRequests.anyRequest().authenticated()
                )
                .csrf(csrf -> csrf.ignoringRequestMatchers(endpointsMatcher))
                .apply(authorizationServerConfigurer)
                .and()
                .apply(usernamePasswordAuthenticationConfigurer);
    }
}
