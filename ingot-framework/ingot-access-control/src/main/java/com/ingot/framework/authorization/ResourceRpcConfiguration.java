package com.ingot.framework.authorization;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import com.ingot.framework.security.config.annotation.web.configurers.InHttpConfigurersAdapter;
import com.ingot.framework.security.oauth2.server.authorization.OnlineTokenService;
import com.ingot.framework.security.oauth2.server.authorization.SessionStoreAvailability;
import com.ingot.framework.security.oauth2.server.resource.authentication.InJwtAuthenticationConverter;
import com.ingot.framework.security.oauth2.server.resource.web.InBearerTokenAuthenticationEntryPoint;
import com.ingot.framework.security.oauth2.server.resource.web.OAuth2InnerResourceFilter;

/**
 * <p>独立资源服务按显式密钥配置开放对象查询端点。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
@AutoConfiguration(after = AuthorizationSdkConfiguration.class)
@EnableConfigurationProperties(ResourceRpcProperties.class)
public class ResourceRpcConfiguration {

    private static final int OBJECT_RPC_CHAIN_ORDER = Ordered.HIGHEST_PRECEDENCE + 5;

    private static final String SECRET_PROPERTY = "secret";

    /**
     * 在通用内部链之前恢复原始在线成员；专用签名仍由对象端点独立校验。
     * 纯字段清单使用服务签名，不进入本成员认证链。
     * @param adapters 既有安全配置
     * @param sessions 在线会话
     * @param availability 会话存储故障策略
     * @param http 独立链构建器
     * @return 仅匹配对象查询 POST 的成员认证链
     * @throws Exception 安全链装配失败
     */
    @Bean
    @Order(OBJECT_RPC_CHAIN_ORDER)
    @ConditionalOnProperty(prefix = ResourceRpcProperties.PREFIX, name = SECRET_PROPERTY)
    @ConditionalOnBean({OnlineTokenService.class, SessionStoreAvailability.class})
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public SecurityFilterChain iamResourceObjectSecurityFilterChain(
            ObjectProvider<InHttpConfigurersAdapter> adapters, OnlineTokenService sessions,
            SessionStoreAvailability availability, HttpSecurity http) throws Exception {
        var adapter = adapters.getIfAvailable();
        if (adapter != null) adapter.apply(http);
        var endpoint = PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, ResourceObjectRpc.PATH);
        http.securityMatcher(endpoint)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(requests -> requests.anyRequest().authenticated())
            .csrf(csrf -> csrf.ignoringRequestMatchers(endpoint))
            .oauth2ResourceServer(resource -> resource
                .authenticationEntryPoint(new InBearerTokenAuthenticationEntryPoint())
                .bearerTokenResolver(new DefaultBearerTokenResolver())
                .jwt(jwt -> jwt.jwtAuthenticationConverter(new InJwtAuthenticationConverter(sessions, availability))));
        http.addFilterAfter(new OAuth2InnerResourceFilter(), ExceptionTranslationFilter.class);
        return http.build();
    }

    /**
     * 装配服务端。
     * @param registry 本服务注册
     * @param mapper JSON
     * @param properties 可信配置
     * @return 端点
     */
    @Bean
    @ConditionalOnProperty(prefix = ResourceRpcProperties.PREFIX, name = SECRET_PROPERTY)
    public ResourceObjectEndpoint resourceObjectEndpoint(ResourceRegistry registry, ObjectMapper mapper,
            ResourceRpcProperties properties) {
        ResourceRpcSigner.sign("", properties.getSecret());
        return new ResourceObjectEndpoint(registry, mapper, properties.getSecret());
    }

}
