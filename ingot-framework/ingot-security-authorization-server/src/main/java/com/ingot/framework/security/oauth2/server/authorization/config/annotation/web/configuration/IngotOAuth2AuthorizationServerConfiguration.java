package com.ingot.framework.security.oauth2.server.authorization.config.annotation.web.configuration;

import com.ingot.framework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerEnhanceConfigurer;
import com.ingot.framework.security.oauth2.server.authorization.token.JwtOAuth2TokenCustomizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * <p>Description  : IngotOAuth2AuthorizationServerConfiguration.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/8.</p>
 * <p>Time         : 5:02 下午.</p>
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class IngotOAuth2AuthorizationServerConfiguration {

    public static final String SECURITY_FILTER_CHAIN_NAME = "authorizationServerSecurityFilterChain";

    // @formatter:off
    public static void applyDefaultSecurity(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                new OAuth2AuthorizationServerConfigurer();
        RequestMatcher defaultMatcher = authorizationServerConfigurer
                .getEndpointsMatcher();

        // 增强配置
        OAuth2AuthorizationServerEnhanceConfigurer enhanceConfigurer =
                new OAuth2AuthorizationServerEnhanceConfigurer();
        RequestMatcher enhanceMatcher = enhanceConfigurer.getEndpointsMatcher();

        // Request merge
        RequestMatcher endpointsMatcher = new OrRequestMatcher(defaultMatcher, enhanceMatcher);

        http.securityMatcher(endpointsMatcher)
                .authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests.anyRequest().authenticated()
                )
                .csrf(csrf -> csrf.ignoringRequestMatchers(endpointsMatcher))
                .with(authorizationServerConfigurer, (configurer) -> {
                    // 自定义配置
                    configurer.tokenEndpoint(new OAuth2TokenEndpointCustomizer())
                            .clientAuthentication(new OAuth2ClientAuthenticationCustomizer())
                            .authorizationEndpoint(new OAuth2AuthorizationServerCustomizer());
                })
                .with(enhanceConfigurer, Customizer.withDefaults());
    }
    // @formatter:on

    @Bean(SECURITY_FILTER_CHAIN_NAME)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ConditionalOnMissingBean(name = {SECURITY_FILTER_CHAIN_NAME})
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        applyDefaultSecurity(http);
        return http.formLogin(Customizer.withDefaults()).build();
    }

    @Bean
    @ConditionalOnMissingBean(OAuth2TokenCustomizer.class)
    public OAuth2TokenCustomizer<JwtEncodingContext> oAuth2TokenCustomizer() {
        return new JwtOAuth2TokenCustomizer();
    }
}
