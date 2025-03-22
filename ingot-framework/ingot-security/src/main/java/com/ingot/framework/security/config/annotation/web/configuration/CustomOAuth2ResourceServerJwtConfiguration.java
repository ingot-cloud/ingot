package com.ingot.framework.security.config.annotation.web.configuration;

import com.ingot.framework.security.core.InSecurityProperties;
import com.ingot.framework.security.oauth2.jwt.CustomJwtValidators;
import com.ingot.framework.security.oauth2.jwt.JwkSupplier;
import com.ingot.framework.security.oauth2.jwt.RedisJwkSupplier;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.oauth2.resource.IssuerUriCondition;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.util.HashSet;
import java.util.Set;

/**
 * <p>Description  : 自定义资源服务器Jwt配置.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/10/8.</p>
 * <p>Time         : 11:09 上午.</p>
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class CustomOAuth2ResourceServerJwtConfiguration {

    /**
     * 参考 {@link org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerJwtConfiguration}
     */
    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    @Conditional(IssuerUriCondition.class)
    JwtDecoder jwtDecoderByIssuerUri(OAuth2ResourceServerProperties properties,
                                     InSecurityProperties inSecurityProperties) {
        log.info("[CustomOAuth2ResourceServerJwtConfiguration] jwtDecoderByIssuerUri, 使用CustomJwtValidators.createDefaultWithIssuer");
        OAuth2ResourceServerProperties.Jwt jwt = properties.getJwt();
        NimbusJwtDecoder jwtDecoder = JwtDecoders.fromIssuerLocation(jwt.getIssuerUri());
        // 扩展 JwtValidator
        jwtDecoder.setJwtValidator(CustomJwtValidators.createDefaultWithIssuer(
                jwt.getIssuerUri(), inSecurityProperties));
        return jwtDecoder;
    }

    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    JwtDecoder jwtDecoder(InSecurityProperties properties,
                          JWKSource<SecurityContext> jwkSource) {
        log.info("[CustomOAuth2ResourceServerJwtConfiguration] default jwtDecoder");
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
                CustomJwtValidators.createDefault(properties));
        return jwtDecoder;
    }

    @Bean
    @ConditionalOnMissingBean(JwkSupplier.class)
    JwkSupplier jwkSupplier(StringRedisTemplate template) {
        log.info("[CustomOAuth2ResourceServerJwtConfiguration] default jwkSupplier");
        return new RedisJwkSupplier(template);
    }

    @Bean
    @ConditionalOnMissingBean(JWKSource.class)
    JWKSource<SecurityContext> jwkSource(JwkSupplier service) {
        JWKSet jwkSet = service.get();
        return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
    }
}
