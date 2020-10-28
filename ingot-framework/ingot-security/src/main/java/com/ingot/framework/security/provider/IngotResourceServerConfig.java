package com.ingot.framework.security.provider;

import com.ingot.framework.security.core.authorize.AuthorizeConfigManager;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.expression.OAuth2WebSecurityExpressionHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.annotation.Resource;

/**
 * <p>Description  : ResourceConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/11.</p>
 * <p>Time         : 下午9:55.</p>
 */
@Slf4j
public class IngotResourceServerConfig extends ResourceServerConfigurerAdapter {
    @Resource
    private AccessDeniedHandler ingotAccessDeniedHandler;
    @Resource
    private OAuth2WebSecurityExpressionHandler ingotSecurityExpressionHandler;
    @Resource
    private AuthenticationEntryPoint ingotAuthenticationEntryPoint;
    @Resource
    private AuthorizeConfigManager authorizeConfigManager;
    @Resource
    private ResourceServerProperties resource;

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
