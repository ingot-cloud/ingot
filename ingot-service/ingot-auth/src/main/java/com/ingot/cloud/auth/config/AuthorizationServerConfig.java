package com.ingot.cloud.auth.config;

import cn.hutool.core.collection.ListUtil;
import com.ingot.cloud.auth.client.InJdbcRegisteredClientRepository;
import com.ingot.framework.security.config.annotation.web.configuration.InOAuth2ResourceServerConfiguration;
import com.ingot.framework.security.config.annotation.web.configurers.InHttpConfigurersAdapter;
import com.ingot.framework.security.oauth2.core.InOAuth2AuthProperties;
import com.ingot.framework.security.oauth2.core.PermitResolver;
import com.ingot.framework.security.oauth2.server.authorization.OnlineTokenService;
import com.ingot.framework.security.oauth2.server.authorization.config.annotation.web.configuration.InOAuth2AuthorizationServerConfiguration;
import com.ingot.framework.tenant.TenantHttpConfigurer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.DelegatingSecurityContextRevokeRepository;
import org.springframework.security.web.context.RedisSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRevokeRepository;

/**
 * <p>Description  : AuthServerConfiguration.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/6.</p>
 * <p>Time         : 11:07 上午.</p>
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class AuthorizationServerConfig {
    private final RedisTemplate<String, Object> redisTemplate;
    private final InOAuth2AuthProperties properties;

    @Bean(InOAuth2AuthorizationServerConfiguration.SECURITY_FILTER_CHAIN_NAME)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http,
                                                                      TenantHttpConfigurer tenantHttpConfigurer) throws Exception {
        InOAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        tenantHttpConfigurer.configure(http);
        http.securityContext(new SecurityContextCustomizer(this.redisTemplate))
                .exceptionHandling(new ExceptionHandlingCustomizer(this.properties));
        return http.build();
    }

    @Bean(InOAuth2ResourceServerConfiguration.SECURITY_FILTER_CHAIN_NAME)
    @ConditionalOnMissingBean(name = {InOAuth2ResourceServerConfiguration.SECURITY_FILTER_CHAIN_NAME})
    public SecurityFilterChain resourceServerSecurityFilterChain(InHttpConfigurersAdapter httpConfigurersAdapter,
                                                                 PermitResolver permitResolver,
                                                                 OnlineTokenService onlineTokenService,
                                                                 HttpSecurity http) throws Exception {
        InOAuth2ResourceServerConfiguration
                .applyDefaultSecurity(httpConfigurersAdapter, permitResolver, onlineTokenService, http);
        http.csrf(new CsrfCustomizer(permitResolver))
                .formLogin(new FormLoginCustomizer())
                .logout(new LogoutCustomizer());
        return http.build();
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        return new InJdbcRegisteredClientRepository(jdbcTemplate);
    }

    @Bean
    public AuthorizationServerSettings providerSettings(InOAuth2AuthProperties properties) {
        return AuthorizationServerSettings.builder().issuer(properties.getIssuer()).build();
    }

    @Bean
    public RedisSecurityContextRepository redisSecurityContextRepository() {
        return new RedisSecurityContextRepository(this.redisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(SecurityContextRevokeRepository.class)
    public SecurityContextRevokeRepository securityContextRevokeRepository(RedisSecurityContextRepository redisSecurityContextRepository) {
        return new DelegatingSecurityContextRevokeRepository(ListUtil.list(false, redisSecurityContextRepository));
    }
}
