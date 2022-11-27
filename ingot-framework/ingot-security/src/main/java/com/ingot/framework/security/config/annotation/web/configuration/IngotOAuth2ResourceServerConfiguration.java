package com.ingot.framework.security.config.annotation.web.configuration;

import java.util.List;

import com.ingot.framework.security.config.annotation.web.configurers.IngotHttpConfigurersAdapter;
import com.ingot.framework.security.config.annotation.web.configurers.oauth2.server.resource.IngotTokenAuthConfigurer;
import com.ingot.framework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2InnerResourceConfigurer;
import com.ingot.framework.security.core.userdetails.DefaultOAuth2UserDetailsServiceManager;
import com.ingot.framework.security.core.userdetails.OAuth2UserDetailsService;
import com.ingot.framework.security.core.userdetails.OAuth2UserDetailsServiceManager;
import com.ingot.framework.security.core.userdetails.RemoteOAuth2UserDetailsService;
import com.ingot.framework.security.core.userdetails.RemoteUserDetailsService;
import com.ingot.framework.security.oauth2.core.IngotOAuth2ResourceProperties;
import com.ingot.framework.security.oauth2.core.PermitResolver;
import com.ingot.framework.security.oauth2.server.authorization.AuthorizationCacheService;
import com.ingot.framework.security.oauth2.server.authorization.DefaultAuthorizationCacheService;
import com.ingot.framework.security.oauth2.server.resource.access.expression.IngotSecurityExpression;
import com.ingot.framework.security.web.ClientContextAwareFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

/**
 * <p>Description  : IngotOAuth2ResourceServerConfiguration.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/15.</p>
 * <p>Time         : 2:37 下午.</p>
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class IngotOAuth2ResourceServerConfiguration {

    public static final String SECURITY_FILTER_CHAIN_NAME = "resourceServerSecurityFilterChain";

    public static void applyDefaultSecurity(PermitResolver permitResolver,
                                            HttpSecurity http) throws Exception {
        applyDefaultSecurity(null, permitResolver, http);
    }

    public static void applyDefaultSecurity(IngotHttpConfigurersAdapter httpConfigurersAdapter,
                                            PermitResolver permitResolver,
                                            HttpSecurity http) throws Exception {
        if (httpConfigurersAdapter != null) {
            httpConfigurersAdapter.apply(http);
        }
        http.authorizeRequests(authorizeRequests -> {
                    permitResolver.permitAllPublic(authorizeRequests);
                    authorizeRequests.anyRequest().authenticated();
                })
                .apply(new IngotTokenAuthConfigurer(permitResolver.publicRequestMatcher()))
                .and()
                .oauth2ResourceServer(new OAuth2ResourceServerCustomizer(permitResolver));
        http.addFilterBefore(new ClientContextAwareFilter(), UsernamePasswordAuthenticationFilter.class);
    }

    @Bean(SECURITY_FILTER_CHAIN_NAME)
    @ConditionalOnMissingBean(name = {SECURITY_FILTER_CHAIN_NAME})
    public SecurityFilterChain resourceServerSecurityFilterChain(IngotHttpConfigurersAdapter httpConfigurersAdapter,
                                                                 PermitResolver permitResolver,
                                                                 HttpSecurity http) throws Exception {
        applyDefaultSecurity(httpConfigurersAdapter, permitResolver, http);
        return http.build();
    }

    @Bean
    @Order(HIGHEST_PRECEDENCE + 10)
    public SecurityFilterChain innerResourceSecurityFilterChain(PermitResolver permitResolver,
                                                                HttpSecurity http) throws Exception {
        OAuth2InnerResourceConfigurer innerResourceConfigurer =
                new OAuth2InnerResourceConfigurer(permitResolver);
        RequestMatcher endpointsMatcher = innerResourceConfigurer
                .getRequestMatcher();

        http.requestMatcher(endpointsMatcher)
                .authorizeRequests(authorizeRequests -> {
                    permitResolver.permitAllInner(authorizeRequests);
                    authorizeRequests.anyRequest().authenticated();
                })
                .csrf(csrf -> csrf.ignoringRequestMatchers(endpointsMatcher))
                .apply(innerResourceConfigurer);
        return http.build();
    }

    @Bean
    @ConditionalOnBean(RemoteUserDetailsService.class)
    @ConditionalOnMissingBean(UserDetailsService.class)
    public OAuth2UserDetailsService userDetailsService(RemoteUserDetailsService remoteUserDetailsService) {
        return new RemoteOAuth2UserDetailsService(remoteUserDetailsService);
    }

    @Bean
    @ConditionalOnBean(OAuth2UserDetailsService.class)
    @ConditionalOnMissingBean(OAuth2UserDetailsServiceManager.class)
    public OAuth2UserDetailsServiceManager userDetailsServiceManager(List<OAuth2UserDetailsService> userDetailsServices) {
        return new DefaultOAuth2UserDetailsServiceManager(userDetailsServices);
    }

    @Bean
    @ConditionalOnMissingBean(AuthorizationCacheService.class)
    public AuthorizationCacheService authorizationCacheService() {
        return new DefaultAuthorizationCacheService();
    }

    @Bean("ingot")
    public IngotSecurityExpression ingotSecurityExpression() {
        return new IngotSecurityExpression();
    }

    @Bean
    public PermitResolver permitResolver(WebApplicationContext context,
                                         IngotOAuth2ResourceProperties ingotOAuth2ResourceProperties) {
        return new PermitResolver(context, ingotOAuth2ResourceProperties);
    }

}
