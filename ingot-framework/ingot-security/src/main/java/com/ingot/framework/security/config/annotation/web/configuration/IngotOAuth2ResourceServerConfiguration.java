package com.ingot.framework.security.config.annotation.web.configuration;

import com.ingot.framework.security.config.annotation.web.configurers.IngotHttpConfigurersAdapter;
import com.ingot.framework.security.config.annotation.web.configurers.oauth2.server.resource.IngotTokenAuthConfigurer;
import com.ingot.framework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2InnerResourceConfigurer;
import com.ingot.framework.security.core.tenantdetails.DefaultTenantDetailsService;
import com.ingot.framework.security.core.tenantdetails.RemoteTenantDetailsService;
import com.ingot.framework.security.core.tenantdetails.TenantDetailsService;
import com.ingot.framework.security.core.userdetails.*;
import com.ingot.framework.security.oauth2.core.IngotOAuth2ResourceProperties;
import com.ingot.framework.security.oauth2.core.PermitResolver;
import com.ingot.framework.security.oauth2.server.authorization.AuthorizationCacheService;
import com.ingot.framework.security.oauth2.server.authorization.DefaultAuthorizationCacheService;
import com.ingot.framework.security.oauth2.server.resource.access.expression.IngotSecurityExpression;
import com.ingot.framework.security.web.ClientContextAwareFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.authorization.method.PrePostTemplateDefaults;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

/**
 * <p>Description  : IngotOAuth2ResourceServerConfiguration.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/15.</p>
 * <p>Time         : 2:37 下午.</p>
 */
@Slf4j
@EnableWebSecurity(debug = true)
@EnableMethodSecurity
@Configuration(proxyBeanMethods = false)
public class IngotOAuth2ResourceServerConfiguration {

    public static final String SECURITY_FILTER_CHAIN_NAME = "resourceServerSecurityFilterChain";

    public static void applyDefaultSecurity(PermitResolver permitResolver,
                                            HttpSecurity http) throws Exception {
        applyDefaultSecurity(null, permitResolver, http);
    }

    public static void applyDefaultSecurity(IngotHttpConfigurersAdapter httpConfigurersAdapter,
                                            PermitResolver permitResolver,
                                            HttpSecurity http) throws Exception {
        if (httpConfigurersAdapter != null) {
            httpConfigurersAdapter.apply(http);
        }
        http.authorizeHttpRequests(authorizeRequests -> {
                    permitResolver.permitAllPublic(authorizeRequests);
                    authorizeRequests.anyRequest().authenticated();
                })
                .csrf(csrf -> csrf.ignoringRequestMatchers(permitResolver.publicRequestMatcher()))
                .oauth2ResourceServer(new OAuth2ResourceServerCustomizer(permitResolver))
                .with(new IngotTokenAuthConfigurer(permitResolver.publicRequestMatcher()), Customizer.withDefaults());
        http.addFilterBefore(new ClientContextAwareFilter(), UsernamePasswordAuthenticationFilter.class);
    }

    @Bean(SECURITY_FILTER_CHAIN_NAME)
    @ConditionalOnMissingBean(name = {SECURITY_FILTER_CHAIN_NAME})
    public SecurityFilterChain resourceServerSecurityFilterChain(IngotHttpConfigurersAdapter httpConfigurersAdapter,
                                                                 PermitResolver permitResolver,
                                                                 HttpSecurity http) throws Exception {
        applyDefaultSecurity(httpConfigurersAdapter, permitResolver, http);
        return http.build();
    }

    @Bean
    @Order(HIGHEST_PRECEDENCE + 10)
    public SecurityFilterChain innerResourceSecurityFilterChain(IngotHttpConfigurersAdapter httpConfigurersAdapter,
                                                                PermitResolver permitResolver,
                                                                HttpSecurity http) throws Exception {
        if (httpConfigurersAdapter != null) {
            httpConfigurersAdapter.apply(http);
        }

        OAuth2InnerResourceConfigurer innerResourceConfigurer =
                new OAuth2InnerResourceConfigurer(permitResolver);
        RequestMatcher endpointsMatcher = innerResourceConfigurer
                .getRequestMatcher();

        http.securityMatcher(endpointsMatcher)
                .authorizeHttpRequests(authorizeRequests -> {
                    permitResolver.permitAllInner(authorizeRequests);
                    authorizeRequests.anyRequest().authenticated();
                })
                .csrf(csrf -> csrf.ignoringRequestMatchers(endpointsMatcher))
                .with(innerResourceConfigurer, Customizer.withDefaults());
        return http.build();
    }

    /**
     * 由于注入了多个{@link UserDetailsService}，为了保证
     * {@link AbstractUserDetailsAuthenticationProvider}可用，增加如下配置
     */
    @Bean
    public InitializeUserDetailsBeanManagerConfigurer ingotInitializeUserDetailsBeanManagerConfigurer(
            ApplicationContext context) {
        return new InitializeUserDetailsBeanManagerConfigurer(context);
    }

    /**
     * 如果注入多个 {@link UserDetailsService}，{@link InitializeUserDetailsBeanManagerConfigurer}
     * 默认使用该 {@link UserDetailsService}
     */
    @Primary
    @Bean
    @ConditionalOnBean(RemoteUserDetailsService.class)
    @ConditionalOnMissingBean(UserDetailsService.class)
    public OAuth2UserDetailsService passwordUserDetailsService(RemoteUserDetailsService remoteUserDetailsService) {
        return new RemoteOAuth2UserDetailsService(remoteUserDetailsService);
    }

    @Bean
    @ConditionalOnBean(OAuth2UserDetailsService.class)
    @ConditionalOnMissingBean(OAuth2UserDetailsServiceManager.class)
    public OAuth2UserDetailsServiceManager userDetailsServiceManager(List<UserDetailsService> userDetailsServices) {
        return new DefaultOAuth2UserDetailsServiceManager(userDetailsServices);
    }

    @Bean
    @ConditionalOnBean(RemoteTenantDetailsService.class)
    @ConditionalOnMissingBean(TenantDetailsService.class)
    public TenantDetailsService tenantDetailsService(RemoteTenantDetailsService remoteTenantDetailsService) {
        return new DefaultTenantDetailsService(remoteTenantDetailsService);
    }

    @Bean
    @ConditionalOnMissingBean(AuthorizationCacheService.class)
    public AuthorizationCacheService authorizationCacheService() {
        return new DefaultAuthorizationCacheService();
    }

    @Bean
    public PermitResolver permitResolver(WebApplicationContext context,
                                         IngotOAuth2ResourceProperties ingotOAuth2ResourceProperties) {
        return new PermitResolver(context, ingotOAuth2ResourceProperties);
    }

    @Bean("ingot")
    public IngotSecurityExpression ingotSecurityExpression() {
        return new IngotSecurityExpression();
    }

    /**
     * 支持自定义权限表达式
     *
     * @return {@link PrePostTemplateDefaults }
     */
    @Bean
    public PrePostTemplateDefaults prePostTemplateDefaults() {
        return new PrePostTemplateDefaults();
    }

}
