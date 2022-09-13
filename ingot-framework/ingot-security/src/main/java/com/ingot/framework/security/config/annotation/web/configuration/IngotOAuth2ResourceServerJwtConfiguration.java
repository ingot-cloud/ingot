package com.ingot.framework.security.config.annotation.web.configuration;

import com.ingot.framework.security.oauth2.jwt.IngotJwtValidators;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.oauth2.resource.IssuerUriCondition;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * <p>Description  : IngotOAuth2ResourceServerJwtConfiguration.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/10/8.</p>
 * <p>Time         : 11:09 上午.</p>
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class IngotOAuth2ResourceServerJwtConfiguration {

    /**
     * 参考 {@link org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerJwtConfiguration}
     */
    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    @Conditional(IssuerUriCondition.class)
    JwtDecoder jwtDecoderByIssuerUri(OAuth2ResourceServerProperties properties) {
        log.info("[IngotOAuth2ResourceServerJwtConfiguration] 扩展 JwtValidator，使用 IngotJwtValidators.createDefaultWithIssuer");
        OAuth2ResourceServerProperties.Jwt jwt = properties.getJwt();
        NimbusJwtDecoder jwtDecoder = JwtDecoders.fromIssuerLocation(jwt.getIssuerUri());
        // 扩展 JwtValidator
        jwtDecoder.setJwtValidator(IngotJwtValidators.createDefaultWithIssuer(jwt.getIssuerUri()));
        return jwtDecoder;
    }
}
