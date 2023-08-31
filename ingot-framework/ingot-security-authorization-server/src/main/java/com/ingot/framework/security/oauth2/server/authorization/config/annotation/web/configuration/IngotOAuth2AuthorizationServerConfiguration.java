package com.ingot.framework.security.oauth2.server.authorization.config.annotation.web.configuration;

import cn.hutool.core.collection.ListUtil;
import com.ingot.framework.security.oauth2.server.authorization.authentication.DelegateUserDetailsTokenProcessor;
import com.ingot.framework.security.oauth2.server.authorization.authentication.UserDetailsTokenConfirmCodeProcessor;
import com.ingot.framework.security.oauth2.server.authorization.authentication.UserDetailsTokenProcessor;
import com.ingot.framework.security.oauth2.server.authorization.code.DefaultPreAuthorizationService;
import com.ingot.framework.security.oauth2.server.authorization.code.PreAuthorizationService;
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

        // 设置 ObjectPostProcessor
        authorizationServerConfigurer
                .withObjectPostProcessor(new OAuth2TokenEndpointFilterPostProcessor())
                .withObjectPostProcessor(new OAuth2ClientAuthenticationFilterPostProcessor());

        // 增强配置
        OAuth2AuthorizationServerEnhanceConfigurer enhanceConfigurer =
                new OAuth2AuthorizationServerEnhanceConfigurer();
        RequestMatcher enhanceMatcher = enhanceConfigurer.getEndpointsMatcher();

        // 合并
        RequestMatcher endpointsMatcher = new OrRequestMatcher(defaultMatcher, enhanceMatcher);

        http.securityMatcher(endpointsMatcher)
                .authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests.anyRequest().authenticated()
                )
                .csrf(csrf -> csrf.ignoringRequestMatchers(endpointsMatcher))
                .apply(authorizationServerConfigurer);
        http.apply(enhanceConfigurer);
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

    @Bean
    @ConditionalOnMissingBean(PreAuthorizationService.class)
    public PreAuthorizationService preAuthorizationCodeService() {
        return new DefaultPreAuthorizationService();
    }

    @Bean
    @ConditionalOnMissingBean(UserDetailsTokenProcessor.class)
    public UserDetailsTokenProcessor userDetailsTokenProcessor(PreAuthorizationService preAuthorizationService) {
        return new DelegateUserDetailsTokenProcessor(ListUtil.list(false,
                new UserDetailsTokenConfirmCodeProcessor(preAuthorizationService)));
    }
}
