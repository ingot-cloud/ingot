package com.ingot.framework.security.config.annotation.web.configuration;

import com.ingot.framework.security.config.annotation.web.configurers.IngotHttpConfigurersAdapter;
import com.ingot.framework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2InnerResourceConfigurer;
import com.ingot.framework.security.core.userdetails.RemoteIngotUserDetailsService;
import com.ingot.framework.security.core.userdetails.RemoteUserDetailsService;
import com.ingot.framework.security.oauth2.core.PermitResolver;
import com.ingot.framework.security.oauth2.server.resource.authentication.IngotJwtAuthenticationConverter;
import com.ingot.framework.security.oauth2.server.resource.web.IngotBearerTokenAuthenticationEntryPoint;
import com.ingot.framework.security.oauth2.server.resource.web.IngotBearerTokenResolver;
import com.ingot.framework.security.web.authentication.ClientAuthContextFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
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
import static org.springframework.security.config.Customizer.withDefaults;

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

    private IngotHttpConfigurersAdapter httpConfigurersAdapter;
    private PermitResolver permitResolver;

    @Bean(SECURITY_FILTER_CHAIN_NAME)
    @ConditionalOnMissingBean(name = {SECURITY_FILTER_CHAIN_NAME})
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        httpConfigurersAdapter.apply(http);
        http.authorizeRequests(authorizeRequests -> {
                    permitResolver.permitAllPublic(authorizeRequests);
                    authorizeRequests.anyRequest().authenticated();
                })
                .oauth2ResourceServer()
                .authenticationEntryPoint(new IngotBearerTokenAuthenticationEntryPoint())
                .bearerTokenResolver(new IngotBearerTokenResolver(permitResolver))
                .jwt()
                .jwtAuthenticationConverter(new IngotJwtAuthenticationConverter());

        return http.formLogin(withDefaults())
                .addFilterBefore(new ClientAuthContextFilter(), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    @Order(HIGHEST_PRECEDENCE + 10)
    public SecurityFilterChain innerResourceSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2InnerResourceConfigurer<HttpSecurity> innerResourceConfigurer =
                new OAuth2InnerResourceConfigurer<>(permitResolver);
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
    public UserDetailsService userDetailsService(RemoteUserDetailsService remoteUserDetailsService) {
        return new RemoteIngotUserDetailsService(remoteUserDetailsService);
    }

    @Bean
    @ConfigurationProperties(prefix = "ingot.oauth2.resource")
    public PermitResolver permitResolver(WebApplicationContext context) {
        return new PermitResolver(context);
    }

    @Autowired
    public void setPermitResolver(PermitResolver permitResolver) {
        this.permitResolver = permitResolver;
    }

    @Autowired
    public void setHttpConfigurersAdapter(IngotHttpConfigurersAdapter httpConfigurersAdapter) {
        this.httpConfigurersAdapter = httpConfigurersAdapter;
    }
}
