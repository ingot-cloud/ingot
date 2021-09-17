package com.ingot.framework.security.config.annotation.web.configuration;

import com.ingot.framework.security.config.annotation.web.configurers.IngotHttpConfigurersAdapter;
import com.ingot.framework.security.core.userdetails.RemoteIngotUserDetailsService;
import com.ingot.framework.security.core.userdetails.RemoteUserDetailsService;
import com.ingot.framework.security.oauth2.server.resource.web.IngotBearerTokenAuthenticationEntryPoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;

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

    @Bean(SECURITY_FILTER_CHAIN_NAME)
    @ConditionalOnMissingBean(name = {SECURITY_FILTER_CHAIN_NAME})
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        httpConfigurersAdapter.apply(http);
        http.authorizeRequests(authorizeRequests ->
                        authorizeRequests.anyRequest().authenticated())
                .oauth2ResourceServer()
                .authenticationEntryPoint(new IngotBearerTokenAuthenticationEntryPoint())
                .jwt();
        return http.build();
    }

    @Bean
    @ConditionalOnBean(RemoteUserDetailsService.class)
    @ConditionalOnMissingBean(UserDetailsService.class)
    public UserDetailsService userDetailsService(RemoteUserDetailsService remoteUserDetailsService) {
        return new RemoteIngotUserDetailsService(remoteUserDetailsService);
    }

    @Autowired
    public void setHttpConfigurersAdapter(IngotHttpConfigurersAdapter httpConfigurersAdapter) {
        this.httpConfigurersAdapter = httpConfigurersAdapter;
    }
}
