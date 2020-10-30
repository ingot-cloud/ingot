package com.ingot.framework.security.provider;

import com.ingot.framework.security.core.authorize.AuthorizeConfigManager;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.expression.OAuth2WebSecurityExpressionHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * <p>Description  : ResourceConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/11.</p>
 * <p>Time         : 下午9:55.</p>
 */
@Slf4j
@AllArgsConstructor
public class IngotResourceServerConfig extends ResourceServerConfigurerAdapter {
    private final AccessDeniedHandler ingotAccessDeniedHandler;
    private final OAuth2WebSecurityExpressionHandler ingotSecurityExpressionHandler;
    private final AuthenticationEntryPoint ingotAuthenticationEntryPoint;
    private final AuthorizeConfigManager authorizeConfigManager;
    private final ResourceServerProperties resource;

    @SneakyThrows
    @Override public void configure(HttpSecurity http) {
        log.info(">> IngotResourceServerConfig [configure] http security");
        http
                .headers()
                    .frameOptions().disable()
                    .and()
                .csrf()
                    .disable();

        authorizeConfigManager.config(http);
    }

    @Override public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
        final String resourceId = resource.getId();
        log.info(">> IngotResourceServerConfig [configure] ========>>> Resource Id: {}", resourceId);
        resources.resourceId(resourceId)
                .expressionHandler(ingotSecurityExpressionHandler)
                .accessDeniedHandler(ingotAccessDeniedHandler)
                .authenticationEntryPoint(ingotAuthenticationEntryPoint);
    }
}
