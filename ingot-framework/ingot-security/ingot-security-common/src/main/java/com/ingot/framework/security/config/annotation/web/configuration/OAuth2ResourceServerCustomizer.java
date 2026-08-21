package com.ingot.framework.security.config.annotation.web.configuration;

import com.ingot.framework.security.oauth2.core.PermitResolver;
import com.ingot.framework.security.oauth2.server.authorization.OnlineTokenService;
import com.ingot.framework.security.oauth2.server.authorization.SessionStoreAvailability;
import com.ingot.framework.security.oauth2.server.resource.authentication.InJwtAuthenticationConverter;
import com.ingot.framework.security.oauth2.server.resource.web.InBearerTokenAuthenticationEntryPoint;
import com.ingot.framework.security.oauth2.server.resource.web.InBearerTokenResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;

/**
 * <p>资源服务器定制：接入自定义 Bearer Token 解析、错误响应与基于在线会话的认证转换。</p>
 *
 * @author wangchao
 * @since 1.0.0
 * @see InJwtAuthenticationConverter
 */
@RequiredArgsConstructor
public class OAuth2ResourceServerCustomizer implements Customizer<OAuth2ResourceServerConfigurer<HttpSecurity>> {
    private final PermitResolver permitResolver;
    private final OnlineTokenService onlineTokenService;
    private final SessionStoreAvailability sessionStoreAvailability;

    @Override
    public void customize(OAuth2ResourceServerConfigurer<HttpSecurity> configurer) {
        configurer.authenticationEntryPoint(new InBearerTokenAuthenticationEntryPoint())
                .bearerTokenResolver(new InBearerTokenResolver(permitResolver))
                .jwt(jwtConfigurer -> jwtConfigurer.jwtAuthenticationConverter(
                        new InJwtAuthenticationConverter(onlineTokenService, sessionStoreAvailability)
                ));
    }
}
