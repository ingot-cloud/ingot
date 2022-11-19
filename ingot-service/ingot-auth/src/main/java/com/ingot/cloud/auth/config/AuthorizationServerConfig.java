package com.ingot.cloud.auth.config;

import java.util.HashSet;
import java.util.Set;

import com.ingot.cloud.auth.client.IngotJdbcRegisteredClientRepository;
import com.ingot.cloud.auth.service.IngotJdbcOAuth2AuthorizationConsentService;
import com.ingot.cloud.auth.service.IngotJdbcOAuth2AuthorizationService;
import com.ingot.cloud.auth.service.JWKService;
import com.ingot.framework.security.oauth2.core.IngotOAuth2AuthProperties;
import com.ingot.framework.security.oauth2.jwt.IngotJwtValidators;
import com.ingot.framework.tenant.filter.TenantFilter;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.HeaderWriterFilter;

import static com.ingot.framework.security.oauth2.server.authorization.config.annotation.web.configuration.IngotOAuth2AuthorizationServerConfiguration.SECURITY_FILTER_CHAIN_NAME;
import static com.ingot.framework.security.oauth2.server.authorization.config.annotation.web.configuration.IngotOAuth2AuthorizationServerConfiguration.applyDefaultSecurity;

/**
 * <p>Description  : AuthServerConfiguration.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/6.</p>
 * <p>Time         : 11:07 上午.</p>
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class AuthorizationServerConfig {

    @Bean(SECURITY_FILTER_CHAIN_NAME)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        applyDefaultSecurity(http);
        http.addFilterAfter(new TenantFilter(), HeaderWriterFilter.class);
        return http.formLogin(new FormLoginCustomizer()).build();
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
    public AuthorizationServerSettings providerSettings(IngotOAuth2AuthProperties properties) {
        return AuthorizationServerSettings.builder().issuer(properties.getIssuer()).build();
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource(JWKService service) {
        JWKSet jwkSet = service.fetch();
        return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource,
                                 AuthorizationServerSettings authorizationServerSettings) {
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
                IngotJwtValidators.createDefaultWithIssuer(authorizationServerSettings.getIssuer()));
        return jwtDecoder;
    }
}
