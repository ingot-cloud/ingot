package com.ingot.cloud.gateway.security;

import com.ingot.cloud.gateway.filter.auth.AuthContextAttributes;
import com.ingot.cloud.gateway.filter.auth.internal.BearerJwtPayloadReader;
import com.ingot.framework.commons.constants.RedisKeyConstants;
import com.ingot.framework.commons.model.support.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * <p>账号锁定 Gateway Filter：uid key 命中时返回 403，fail-open。</p>
 *
 * <p>userId / userType 优先取 {@link ClientIdentity}（由上游 JWT + 会话补全），
 * 其次取 {@link AuthContextAttributes}；仍缺 userType 则放行，锁定判定交由下游资源服务器。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(AccountLockGatewayProperties.class)
public class AccountLockFilter implements GlobalFilter, Ordered {

    private final AccountLockGatewayProperties properties;
    private final ObjectProvider<ReactiveStringRedisTemplate> redisProvider;
    private final ReactiveResponseWriter responseWriter;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!properties.isEnabled()) {
            return chain.filter(exchange);
        }
        String path = exchange.getRequest().getURI().getPath();
        if (isExcluded(path)) {
            return chain.filter(exchange);
        }

        String userId = resolveUserId(exchange);
        String userType = resolveUserType(exchange);
        if (!StringUtils.hasText(userId) || !StringUtils.hasText(userType)) {
            if (StringUtils.hasText(userId) && !StringUtils.hasText(userType)) {
                log.debug("[AccountLockFilter] skip, userType unresolved path={}", path);
            }
            return chain.filter(exchange);
        }

        Long userIdLong;
        try {
            userIdLong = Long.parseLong(userId);
        } catch (NumberFormatException ex) {
            return chain.filter(exchange);
        }

        ReactiveStringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return chain.filter(exchange);
        }

        String key = RedisKeyConstants.AccountLock.uidKey(userType, userIdLong);
        return redis.hasKey(key)
                .onErrorResume(ex -> {
                    log.warn("[AccountLockFilter] redis check fail-open path={}: {}", path, ex.toString());
                    return Mono.just(false);
                })
                .flatMap(locked -> {
                    if (!Boolean.TRUE.equals(locked)) {
                        return chain.filter(exchange);
                    }
                    log.info("[AccountLockFilter] blocked userType={} userId={}", userType, userId);
                    return responseWriter.writeJson(exchange.getResponse(), HttpStatus.FORBIDDEN,
                            R.error(GatewaySecurityConstants.CODE_ACCOUNT_LOCKED,
                                    GatewaySecurityConstants.MSG_ACCOUNT_LOCKED));
                });
    }

    private static String resolveUserId(ServerWebExchange exchange) {
        ClientIdentity identity = exchange.getAttribute(GatewaySecurityConstants.ATTR_CLIENT_IDENTITY);
        if (identity != null && StringUtils.hasText(identity.getUserId())) {
            return identity.getUserId();
        }
        Object attr = exchange.getAttributes().get(AuthContextAttributes.USER_ID);
        if (attr instanceof String text && StringUtils.hasText(text)) {
            return text;
        }
        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        return BearerJwtPayloadReader.readUserId(authorization);
    }

    private static String resolveUserType(ServerWebExchange exchange) {
        ClientIdentity identity = exchange.getAttribute(GatewaySecurityConstants.ATTR_CLIENT_IDENTITY);
        if (identity != null && StringUtils.hasText(identity.getUserType())) {
            return identity.getUserType();
        }
        Object attr = exchange.getAttributes().get(AuthContextAttributes.USER_TYPE);
        return attr instanceof String text && StringUtils.hasText(text) ? text : null;
    }

    private boolean isExcluded(String path) {
        if (path == null || properties.getExcludePathPatterns() == null) {
            return false;
        }
        for (String pattern : properties.getExcludePathPatterns()) {
            if (StringUtils.hasText(pattern) && pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getOrder() {
        return SecurityPolicyFilterOrder.ACCOUNT_LOCK;
    }
}
