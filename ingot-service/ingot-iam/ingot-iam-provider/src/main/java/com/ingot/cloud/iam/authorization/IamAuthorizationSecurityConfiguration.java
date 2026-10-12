package com.ingot.cloud.iam.authorization;

import java.util.List;

import com.ingot.framework.security.config.annotation.web.configurers.InHttpConfigurersAdapter;
import com.ingot.framework.security.oauth2.server.authorization.OnlineTokenService;
import com.ingot.framework.security.oauth2.server.authorization.SessionStoreAvailability;
import com.ingot.framework.security.oauth2.server.resource.authentication.InJwtAuthenticationConverter;
import com.ingot.framework.security.oauth2.server.resource.web.InBearerTokenAuthenticationEntryPoint;
import com.ingot.framework.security.oauth2.server.resource.web.OAuth2InnerResourceFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.HeaderWriterFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * <p>内部授权 RPC 同时校验内部调用来源和在线认证成员，不使用仅校验调用头的通用 INNER 链。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
public class IamAuthorizationSecurityConfiguration {
    private static final int AUTHORIZATION_CHAIN_ORDER = Ordered.HIGHEST_PRECEDENCE + 5;
    private static final List<String> AUTHORIZATION_ENDPOINTS = List.of(
            "/inner/authorization/snapshot", "/inner/authorization/v2/evaluate", "/inner/authorization/v2/preview");

    /**
     * 在通用 INNER 链之前恢复转发 Token 的在线会话身份；快照自身提供改密状态，避免递归授权。
     * @param adapter 现有服务安全配置
     * @param sessions 在线会话读取
     * @param availability 会话存储故障策略
     * @param http 独立安全链构建器
     * @return 仅匹配三个 POST 授权端点的认证链
     * @throws Exception 安全链装配失败
     */
    @Bean
    @Order(AUTHORIZATION_CHAIN_ORDER)
    public SecurityFilterChain iamAuthorizationSecurityFilterChain(InHttpConfigurersAdapter adapter,
            OnlineTokenService sessions, SessionStoreAvailability availability, HttpSecurity http) throws Exception {
        adapter.apply(http);
        var endpoints = new OrRequestMatcher(AUTHORIZATION_ENDPOINTS.stream()
                .<RequestMatcher>map(path -> PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, path)).toList());
        http.securityMatcher(endpoints)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests.anyRequest().authenticated())
                .csrf(csrf -> csrf.ignoringRequestMatchers(endpoints))
                .oauth2ResourceServer(resource -> resource
                        .authenticationEntryPoint(new InBearerTokenAuthenticationEntryPoint())
                        // 通用解析器忽略 INNER；授权 RPC 必须解析转发的 Token。
                        .bearerTokenResolver(new DefaultBearerTokenResolver())
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(new InJwtAuthenticationConverter(sessions, availability))));
        http.addFilterAfter(new OAuth2InnerResourceFilter(), HeaderWriterFilter.class);
        return http.build();
    }
}
