package com.ingot.cloud.auth.config;

import cn.hutool.core.collection.ListUtil;
import com.ingot.cloud.auth.client.IngotJdbcRegisteredClientRepository;
import com.ingot.cloud.auth.service.IngotJdbcOAuth2AuthorizationConsentService;
import com.ingot.cloud.auth.service.IngotJdbcOAuth2AuthorizationService;
import com.ingot.cloud.auth.service.JWKService;
import com.ingot.framework.security.config.annotation.web.configuration.InOAuth2ResourceServerConfiguration;
import com.ingot.framework.security.config.annotation.web.configurers.IngotHttpConfigurersAdapter;
import com.ingot.framework.security.core.InSecurityProperties;
import com.ingot.framework.security.oauth2.core.InOAuth2AuthProperties;
import com.ingot.framework.security.oauth2.core.PermitResolver;
import com.ingot.framework.security.oauth2.jwt.InJwtValidators;
import com.ingot.framework.security.oauth2.server.authorization.config.annotation.web.configuration.IngotOAuth2AuthorizationServerConfiguration;
import com.ingot.framework.tenant.TenantHttpConfigurer;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
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
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.DelegatingSecurityContextRevokeRepository;
import org.springframework.security.web.context.RedisSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRevokeRepository;

import java.util.HashSet;
import java.util.Set;

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

    @Bean(IngotOAuth2AuthorizationServerConfiguration.SECURITY_FILTER_CHAIN_NAME)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http,
                                                                      TenantHttpConfigurer tenantHttpConfigurer) throws Exception {
        IngotOAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        tenantHttpConfigurer.configure(http);
        http.securityContext(new SecurityContextCustomizer(this.redisTemplate))
                .exceptionHandling(new ExceptionHandlingCustomizer(this.properties));
        return http.build();
    }

    @Bean(InOAuth2ResourceServerConfiguration.SECURITY_FILTER_CHAIN_NAME)
    @ConditionalOnMissingBean(name = {InOAuth2ResourceServerConfiguration.SECURITY_FILTER_CHAIN_NAME})
    public SecurityFilterChain resourceServerSecurityFilterChain(IngotHttpConfigurersAdapter httpConfigurersAdapter,
                                                                 PermitResolver permitResolver,
                                                                 HttpSecurity http) throws Exception {
        InOAuth2ResourceServerConfiguration
                .applyDefaultSecurity(httpConfigurersAdapter, permitResolver, http);
        http.csrf(new CsrfCustomizer(permitResolver))
                .formLogin(new FormLoginCustomizer())
                .logout(new LogoutCustomizer());
        return http.build();
    }

    @Bean
    public OAuth2AuthorizationService authorizationService(JdbcTemplate jdbcTemplate,
                                                           RegisteredClientRepository registeredClientRepository) {
        return new IngotJdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
    }

    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(JdbcTemplate jdbcTemplate,
                                                                         RegisteredClientRepository registeredClientRepository) {
        return new IngotJdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        return new IngotJdbcRegisteredClientRepository(jdbcTemplate);
    }

    @Bean
    public AuthorizationServerSettings providerSettings(InOAuth2AuthProperties properties) {
        return AuthorizationServerSettings.builder().issuer(properties.getIssuer()).build();
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource(JWKService service) {
        JWKSet jwkSet = service.fetch();
        return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource,
                                 AuthorizationServerSettings authorizationServerSettings,
                                 InSecurityProperties properties) {
        Set<JWSAlgorithm> jwsAlgs = new HashSet<>();
        jwsAlgs.addAll(JWSAlgorithm.Family.RSA);
        jwsAlgs.addAll(JWSAlgorithm.Family.EC);
        jwsAlgs.addAll(JWSAlgorithm.Family.HMAC_SHA);
        ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
        JWSKeySelector<SecurityContext> jwsKeySelector =
                new JWSVerificationKeySelector<>(jwsAlgs, jwkSource);
        jwtProcessor.setJWSKeySelector(jwsKeySelector);
        // Override the default Nimbus claims set verifier as NimbusJwtDecoder handles it instead
        jwtProcessor.setJWTClaimsSetVerifier((claims, context) -> {
        });

        NimbusJwtDecoder jwtDecoder = new NimbusJwtDecoder(jwtProcessor);
        // 扩展 JwtValidator
        jwtDecoder.setJwtValidator(
                InJwtValidators.createDefaultWithIssuer(
                        authorizationServerSettings.getIssuer(), properties));
        return jwtDecoder;
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
