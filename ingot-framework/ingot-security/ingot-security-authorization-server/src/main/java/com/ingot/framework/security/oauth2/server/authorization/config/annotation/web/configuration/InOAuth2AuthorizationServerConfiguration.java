package com.ingot.framework.security.oauth2.server.authorization.config.annotation.web.configuration;

import com.ingot.framework.security.core.userdetails.OAuth2UserDetailsServiceManager;
import com.ingot.framework.security.oauth2.server.authorization.OnlineTokenService;
import com.ingot.framework.security.oauth2.server.authorization.RedisOAuth2AuthorizationConsentService;
import com.ingot.framework.security.oauth2.server.authorization.RedisOAuth2AuthorizationService;
import com.ingot.framework.security.oauth2.server.authorization.authentication.AuthenticatedMemberBinder;
import com.ingot.framework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerEnhanceConfigurer;
import com.ingot.framework.security.oauth2.server.authorization.session.DefaultSessionRevocationService;
import com.ingot.framework.security.oauth2.server.authorization.session.SessionRegistrar;
import com.ingot.framework.security.oauth2.server.authorization.session.SessionRevocationListener;
import com.ingot.framework.security.oauth2.server.authorization.session.SessionRevocationService;
import com.ingot.framework.security.oauth2.server.authorization.session.concurrency.SessionConcurrencyEnforcer;
import com.ingot.framework.security.oauth2.server.authorization.token.JwtOAuth2TokenCustomizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * <p>Description  : OAuth2授权服务配置.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/8.</p>
 * <p>Time         : 5:02 下午.</p>
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class InOAuth2AuthorizationServerConfiguration {

    public static final String SECURITY_FILTER_CHAIN_NAME = "authorizationServerSecurityFilterChain";

    // @formatter:off
    public static void applyDefaultSecurity(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                new OAuth2AuthorizationServerConfigurer();
        RequestMatcher defaultMatcher = authorizationServerConfigurer
                .getEndpointsMatcher();

        // 增强配置
        OAuth2AuthorizationServerEnhanceConfigurer enhanceConfigurer =
                new OAuth2AuthorizationServerEnhanceConfigurer();
        RequestMatcher enhanceMatcher = enhanceConfigurer.getEndpointsMatcher();

        // Request merge
        RequestMatcher endpointsMatcher = new OrRequestMatcher(defaultMatcher, enhanceMatcher);

        http.securityMatcher(endpointsMatcher)
                .authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests.anyRequest().authenticated()
                )
                .csrf(csrf -> csrf.ignoringRequestMatchers(endpointsMatcher))
                .with(authorizationServerConfigurer, (configurer) -> {
                    // 自定义配置
                    configurer.tokenEndpoint(new OAuth2TokenEndpointCustomizer())
                            .clientAuthentication(new OAuth2ClientAuthenticationCustomizer())
                            .authorizationEndpoint(new OAuth2AuthorizationServerCustomizer(optionalMemberBinder(http)));
                })
                .with(enhanceConfigurer, (configurer) -> {
                    // 自定义配置
                    configurer.preAuthorizationEndpoint(new OAuth2PreAuthorizationEndpointCustomizer());
                });
    }
    // @formatter:on

    @Bean(SECURITY_FILTER_CHAIN_NAME)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ConditionalOnMissingBean(name = {SECURITY_FILTER_CHAIN_NAME})
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        applyDefaultSecurity(http);
        return http.formLogin(Customizer.withDefaults()).build();
    }

    /**
     * 会话撤销领域服务：彻底撤销的唯一入口。
     * <p>撤销回调可选，未注册实现时撤销仍然生效，只是不产生安全事件。</p>
     */
    @Bean
    @ConditionalOnMissingBean(SessionRevocationService.class)
    public SessionRevocationService sessionRevocationService(OAuth2AuthorizationService authorizationService,
                                                            OnlineTokenService onlineTokenService,
                                                            ObjectProvider<SessionRevocationListener> listeners) {
        log.info("[InOAuth2AuthorizationServerConfiguration] Creating DefaultSessionRevocationService");
        return new DefaultSessionRevocationService(authorizationService, onlineTokenService,
                listeners.orderedStream().toList());
    }

    /**
     * 会话注册入口：签发链上落地会话并执行并发会话约束
     * <p>约束的判定与执行在 {@link SessionConcurrencyEnforcer}，由
     * {@link SessionConcurrencyConfiguration} 按部署形态装配策略来源。</p>
     */
    @Bean
    @ConditionalOnMissingBean(SessionRegistrar.class)
    public SessionRegistrar sessionRegistrar(OnlineTokenService onlineTokenService,
                                            SessionConcurrencyEnforcer sessionConcurrencyEnforcer) {
        log.info("[InOAuth2AuthorizationServerConfiguration] Creating SessionRegistrar");
        return new SessionRegistrar(onlineTokenService, sessionConcurrencyEnforcer);
    }

    /**
     * 租户签发前绑定 IAM 成员上下文。
     *
     * @param users 身份加载器；测试或无远程身份服务时可为缺省
     * @return 成员绑定器
     */
    @Bean
    @ConditionalOnMissingBean
    public AuthenticatedMemberBinder authenticatedMemberBinder(
            ObjectProvider<OAuth2UserDetailsServiceManager> users) {
        return new AuthenticatedMemberBinder(users.getIfAvailable());
    }

    /**
     * JWT Token定制器
     */
    @Bean
    @ConditionalOnMissingBean(OAuth2TokenCustomizer.class)
    public OAuth2TokenCustomizer<JwtEncodingContext> oAuth2TokenCustomizer(SessionRegistrar sessionRegistrar,
                                                                          AuthenticatedMemberBinder memberBinder) {
        log.info("[InOAuth2AuthorizationServerConfiguration] Creating JwtOAuth2TokenCustomizer");
        return new JwtOAuth2TokenCustomizer(sessionRegistrar, memberBinder);
    }

    /**
     * Redis模式的OAuth2AuthorizationService
     * 使用 AuthorizationSnapshot 避免序列化问题
     */
    @Bean
    @ConditionalOnMissingBean(OAuth2AuthorizationService.class)
    public OAuth2AuthorizationService authorizationService(
            RedisTemplate<String, Object> redisTemplate,
            OnlineTokenService onlineTokenService,
            RegisteredClientRepository registeredClientRepository) {
        log.info("[InOAuth2AuthorizationServerConfiguration] Creating RedisOAuth2AuthorizationService with AuthorizationSnapshot");
        return new RedisOAuth2AuthorizationService(
                redisTemplate, onlineTokenService, registeredClientRepository
        );
    }

    /**
     * Redis模式的OAuth2AuthorizationConsentService
     */
    @Bean
    @ConditionalOnMissingBean(OAuth2AuthorizationConsentService.class)
    public OAuth2AuthorizationConsentService authorizationConsentService(
            RedisTemplate<String, Object> redisTemplate,
            RegisteredClientRepository registeredClientRepository) {
        log.info("[InOAuth2AuthorizationServerConfiguration] Creating RedisOAuth2AuthorizationConsentService");
        return new RedisOAuth2AuthorizationConsentService(
                redisTemplate, registeredClientRepository
        );
    }

    private static AuthenticatedMemberBinder optionalMemberBinder(HttpSecurity http) {
        ApplicationContext context = http.getSharedObject(ApplicationContext.class);
        if (context == null) {
            return null;
        }
        return context.getBeanProvider(AuthenticatedMemberBinder.class).getIfAvailable();
    }
}
