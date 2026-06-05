package com.ingot.cloud.gateway.filter;

import java.util.List;
import java.util.Optional;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.commons.constants.BffConstants;
import com.ingot.framework.commons.constants.CacheConstants;
import com.ingot.framework.commons.constants.HeaderConstants;
import com.ingot.framework.commons.constants.SecurityConstants;
import com.ingot.cloud.gateway.filter.GatewayFilterOrders;
import com.ingot.framework.commons.model.bff.BffSession;
import com.ingot.framework.commons.model.status.BaseErrorCode;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.utils.CookieUtil;
import com.ingot.framework.commons.utils.FingerprintUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * <p>网关 Session-to-JWT 转换全局过滤器，实现 BFF Cookie 会话到 Bearer Token 的自动桥接</p>
 *
 * <p>处理逻辑：</p>
 * <ol>
 *     <li>白名单路径（BFF 登录/选租户）→ 直接放行</li>
 *     <li>请求已携带 {@code Authorization: Bearer} → 直接放行（兼容标准 OAuth2 客户端）</li>
 *     <li>无 Bearer → 读取 Cookie {@code IN_SESSION} → 查 Redis
 *         → 反序列化为 {@link BffSession} → <strong>校验客户端指纹</strong>
 *         → 注入 {@code Authorization: Bearer JWT} → 转发下游</li>
 * </ol>
 *
 * <p>指纹校验在网关层执行。优先从 {@code X-In-Ca-Sig} Header 读取前端设备指纹，
 * 读取不到时降级为服务端 IP+UA 计算。确保所有经过 session 转换的请求都受到保护，
 * 即使攻击者窃取了 Cookie，也无法从不同设备发起请求。</p>
 *
 * @author jy
 * @implNote 使用 {@link ReactiveStringRedisTemplate} 异步查询 Redis，
 * 不阻塞 Gateway 的 Reactor 事件循环。Order 为 {@link GatewayFilterOrders#SESSION_RELAY}，
 * 确保在 {@code RequestGlobalFilter} 之后、{@code filter.auth.AuthContextRelayFilter} 之前执行。
 * @see BffSession
 * @see FingerprintUtil
 * @see CacheConstants#bffSessionKey(String)
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionTokenRelayFilter implements GlobalFilter, Ordered {
    private static final List<String> WHITELIST_PATHS = List.of(
            BffConstants.BFF_URL,
            BffConstants.BFF_ORG_SELECT
    );

    private final ReactiveStringRedisTemplate reactiveRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (isWhitelisted(path)) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StrUtil.isNotEmpty(authHeader) && StrUtil.startWithIgnoreCase(authHeader, SecurityConstants.OAUTH2_BEARER_TYPE_WITH_SPACE)) {
            return chain.filter(exchange);
        }

        String sessionId = CookieUtil.getCookieFirstValue(request, CacheConstants.BFF_SESSION_COOKIE_NAME);
        if (StrUtil.isEmpty(sessionId)) {
            return chain.filter(exchange);
        }

        return reactiveRedisTemplate.opsForValue().get(CacheConstants.bffSessionKey(sessionId))
                .map(value -> Optional.ofNullable(deserialize(value)))
                .defaultIfEmpty(Optional.empty())
                .flatMap(optSession -> {
                    if (optSession.isEmpty()) {
                        return chain.filter(exchange);
                    }

                    BffSession session = optSession.get();

                    // 指纹校验
                    if (StrUtil.isNotEmpty(session.getFingerprint())) {
                        String currentFp = resolveFingerprint(request);
                        if (!StrUtil.equals(session.getFingerprint(), currentFp)) {
                            log.warn("[SessionTokenRelay] fingerprint mismatch, session={}", sessionId);
                            return unauthorizedResponse(exchange);
                        }
                    }

                    if (StrUtil.isEmpty(session.getAccessToken())) {
                        log.debug("[SessionTokenRelay] no accessToken in session={}", sessionId);
                        return unauthorizedResponse(exchange);
                    }

                    // 默认使用请求中的Tenant，如果不存在那么传递Session保存的
                    String tenant = request.getHeaders().getFirst(HeaderConstants.TENANT);
                    if (StrUtil.isEmpty(tenant)) {
                        tenant = session.getTenantId();
                    }

                    ServerHttpRequest mutated = request.mutate()
                            .header(HttpHeaders.AUTHORIZATION,
                                    SecurityConstants.OAUTH2_BEARER_TYPE_WITH_SPACE + session.getAccessToken())
                            .header(HeaderConstants.TENANT, tenant)
                            .build();
                    return chain.filter(exchange.mutate().request(mutated).build());
                });
    }

    @Override
    public int getOrder() {
        return GatewayFilterOrders.SESSION_RELAY;
    }

    private boolean isWhitelisted(String path) {
        return WHITELIST_PATHS.stream().anyMatch(path::startsWith);
    }

    private BffSession deserialize(String json) {
        try {
            return objectMapper.readValue(json, BffSession.class);
        } catch (Exception e) {
            log.warn("[SessionTokenRelay] session deserialize failed", e);
            return null;
        }
    }

    private String resolveFingerprint(ServerHttpRequest request) {
        String deviceFp = request.getHeaders().getFirst(HeaderConstants.BFF_DEVICE_FINGERPRINT_HEADER);
        if (StrUtil.isNotEmpty(deviceFp)) {
            return deviceFp;
        }
        String ip = request.getHeaders().getFirst(HeaderConstants.CLIENT_REAL_IP);
        String ua = request.getHeaders().getFirst(HttpHeaders.USER_AGENT);
        return FingerprintUtil.compute(ip, ua);
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        R<?> r = R.error(BaseErrorCode.UNAUTHORIZED.getCode(), BaseErrorCode.UNAUTHORIZED.getText());
        return response.writeWith(Mono.fromSupplier(() -> {
            DataBufferFactory bufferFactory = response.bufferFactory();
            try {
                return bufferFactory.wrap(objectMapper.writeValueAsBytes(r));
            } catch (Exception e) {
                log.error("[SessionTokenRelay] failed to write unauthorized response", e);
                return bufferFactory.wrap(new byte[0]);
            }
        }));
    }
}
