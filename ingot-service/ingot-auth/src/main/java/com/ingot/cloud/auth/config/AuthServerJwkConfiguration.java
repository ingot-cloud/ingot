package com.ingot.cloud.auth.config;

import com.ingot.framework.security.core.InSecurityProperties;
import com.ingot.framework.security.oauth2.jwt.AuthServerJwkSupplier;
import com.ingot.framework.security.oauth2.jwt.JwkSupplier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * <p>Description  : 授权服务器 JWK 配置.</p>
 * <p>使用 AuthServerJwkSupplier 持有私钥用于签名</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/6.</p>
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class AuthServerJwkConfiguration {

    /**
     * 授权服务器专用的 JwkSupplier
     * 覆盖默认的 ResourceServerJwkSupplier
     */
    @Bean
    public JwkSupplier authServerJwkSupplier(StringRedisTemplate template,
                                             InSecurityProperties properties) {
        log.info("[AuthServerJwkConfiguration] Using AuthServerJwkSupplier with private keys");
        return new AuthServerJwkSupplier(template, properties);
    }

    /**
     * 授权服务器专用的 JWKSource
     * 支持密钥动态刷新
     */
    @Bean
    public JWKSource<SecurityContext> authServerJwkSource(JwkSupplier jwkSupplier) {
        log.info("[AuthServerJwkConfiguration] Creating JWKSource with dynamic refresh support");
        // 每次都动态获取，支持密钥轮换
        return (jwkSelector, securityContext) -> {
            JWKSet jwkSet = jwkSupplier.get();
            return jwkSelector.select(jwkSet);
        };
    }
}

