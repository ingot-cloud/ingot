package com.ingot.framework.security.oauth2.server.authorization.config.annotation.web.configuration;

import com.ingot.framework.security.oauth2.jwt.AuthServerJwkSupplier;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * <p>Description  : 授权服务器 JwtEncoder 配置，多密钥场景下通过 JwkSelector 选择当前签名密钥.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2026/7/13.</p>
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class AuthServerJwtEncoderConfiguration {

    @Bean
    @ConditionalOnBean(AuthServerJwkSupplier.class)
    @ConditionalOnMissingBean(JwtEncoder.class)
    JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource,
                          AuthServerJwkSupplier jwkSupplier) {
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(jwkSource);
        encoder.setJwkSelector(jwks -> jwkSupplier.getCurrentSigningKey());
        log.info("[AuthServerJwtEncoderConfiguration] JwtEncoder configured with current-key signing selector");
        return encoder;
    }
}
