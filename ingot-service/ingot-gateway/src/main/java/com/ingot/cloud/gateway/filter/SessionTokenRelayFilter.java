package com.ingot.cloud.gateway.filter;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.cloud.gateway.config.GatewayBffProperties;
import com.ingot.framework.commons.constants.BffConstants;
import com.ingot.framework.commons.constants.BffRedisKeys;
import com.ingot.framework.commons.constants.BffRedisScripts;
import com.ingot.framework.commons.constants.HeaderConstants;
import com.ingot.framework.commons.constants.RedisKeyConstants;
import com.ingot.framework.commons.constants.SecurityConstants;
import com.ingot.framework.commons.model.bff.BffAppRegistration;
import com.ingot.framework.commons.model.bff.BffFingerprintMode;
import com.ingot.framework.commons.model.bff.BffSession;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.status.BaseErrorCode;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.utils.BffCookiePolicy;
import com.ingot.framework.commons.utils.BffOriginPolicy;
import com.ingot.framework.commons.utils.CookieUtil;
import com.ingot.framework.commons.utils.FingerprintUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * <p>
 * 按当前应用中继有效的BFF v2会话，验证代次、指纹和Auth sid并确认真实Cookie交付。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class SessionTokenRelayFilter implements GlobalFilter, Ordered {

    private static final Pattern OPAQUE_ID = Pattern.compile(BffConstants.OPAQUE_ID_REGEX);

    private final ReactiveStringRedisTemplate redis;

    private final ObjectMapper mapper;

    private final GatewayBffProperties properties;

    /** 安全决定与下游执行分离，避免下游异常被误改写成认证故障。 */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (anonymous(request.getURI().getPath())) {
            return chain.filter(exchange.mutate()
                .request(request.mutate().headers(h -> h.remove(HttpHeaders.AUTHORIZATION)).build())
                .build());
        }
        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization != null && !authorization.isBlank())
            return chain.filter(exchange);
        BffAppRegistration app = BffOriginPolicy.match(request.getHeaders().getFirst(HttpHeaders.HOST),
                properties.getApps());
        if (app == null)
            return chain.filter(exchange);
        String id = CookieUtil.getCookieFirstValue(request,
                BffCookiePolicy.sessionCookieName(properties.isRequireHttps(), app.getAppId()));
        if (id == null || !OPAQUE_ID.matcher(id).matches())
            return chain.filter(exchange);
        // Cookie转Bearer的写请求也必须来自本源，覆盖同站不同端口/兄弟域的CSRF风险。
        if (request.getMethod() != org.springframework.http.HttpMethod.GET
                && request.getMethod() != org.springframework.http.HttpMethod.HEAD
                && request.getMethod() != org.springframework.http.HttpMethod.OPTIONS) {
            try {
                if (!app.getOrigin().equals(BffOriginPolicy.normalize(request.getHeaders().getOrigin())))
                    return error(exchange, HttpStatus.FORBIDDEN);
            }
            catch (IllegalArgumentException exception) {
                return error(exchange, HttpStatus.FORBIDDEN);
            }
        }
        return decide(app, id, request).onErrorReturn(new Decision(null, HttpStatus.SERVICE_UNAVAILABLE))
            .flatMap(decision -> {
                if (decision.error() != null)
                    return error(exchange, decision.error());
                BffSession session = decision.session();
                if (session == null)
                    return chain.filter(exchange);
                var builder = request.mutate()
                    .header(HttpHeaders.AUTHORIZATION,
                            SecurityConstants.OAUTH2_BEARER_TYPE_WITH_SPACE + session.getAccessToken());
                String tenant = request.getHeaders().getFirst(HeaderConstants.TENANT);
                if (app.getDomain() == AuthorizationDomain.PLATFORM) {
                    if (tenant != null && !tenant.isBlank())
                        return error(exchange, HttpStatus.UNAUTHORIZED);
                }
                else {
                    if (tenant != null && !tenant.isBlank() && !tenant.equals(session.getTenantId()))
                        return error(exchange, HttpStatus.UNAUTHORIZED);
                    builder.header(HeaderConstants.TENANT, session.getTenantId());
                }
                return chain.filter(exchange.mutate().request(builder.build()).build());
            });
    }

    private Mono<Decision> decide(BffAppRegistration app, String id, ServerHttpRequest request) {
        String appId = app.getAppId();
        String now = Long.toString(Instant.now().getEpochSecond());
        return redis
            .execute(new DefaultRedisScript<>(BffRedisScripts.READ_SESSION, String.class),
                    List.of(BffRedisKeys.session(appId, id)), List.of(appId, now, BffRedisKeys.prefix(appId)))
            .next()
            .map(Optional::of)
            .defaultIfEmpty(Optional.empty())
            .flatMap(raw -> {
                if (raw.isEmpty())
                    return Mono.just(new Decision(null, HttpStatus.UNAUTHORIZED));
                BffSession session;
                try {
                    session = mapper.readValue(raw.get(), BffSession.class);
                }
                catch (Exception exception) {
                    return Mono.just(new Decision(null, HttpStatus.UNAUTHORIZED));
                }
                if (!app.getDomain().getValue().equals(session.getDomain())
                        || !app.getOauthClientId().equals(session.getClientId()) || session.getSid() == null
                        || session.getAccessToken() == null || !fingerprint(session, request))
                    return Mono.just(new Decision(null, HttpStatus.UNAUTHORIZED));
                return redis.hasKey(RedisKeyConstants.OnlineToken.sidKey(session.getSid()))
                    .defaultIfEmpty(false)
                    .flatMap(online -> {
                        if (!online)
                            return Mono.just(new Decision(null, HttpStatus.UNAUTHORIZED));
                        return redis
                            .execute(new DefaultRedisScript<>(BffRedisScripts.CONFIRM_SESSION, Long.class),
                                    List.of(BffRedisKeys.session(appId, id), BffRedisKeys.delivered(appId, id),
                                            BffRedisKeys.current(appId, session.getBindingId())),
                                    List.of(appId, now))
                            .next()
                            .defaultIfEmpty(0L)
                            .map(ok -> new Decision(ok == 1 ? session : null,
                                    ok == 1 ? null : HttpStatus.UNAUTHORIZED));
                    });
            });
    }

    private boolean fingerprint(BffSession s, ServerHttpRequest request) {
        if (s.getFingerprint() == null)
            return true;
        String current = s.getFingerprintMode() == BffFingerprintMode.DEVICE
                ? request.getHeaders().getFirst(HeaderConstants.BFF_DEVICE_FINGERPRINT_HEADER)
                : s.getFingerprintMode() == BffFingerprintMode.IPUA
                        ? FingerprintUtil.compute(request.getHeaders().getFirst(HeaderConstants.INNER_CLIENT_REAL_IP),
                                request.getHeaders().getFirst(HttpHeaders.USER_AGENT))
                        : null;
        return s.getFingerprint().equals(current);
    }

    private boolean anonymous(String path) {
        return (path.equals(BffConstants.PLATFORM_TRANSACTIONS)
                || path.startsWith(BffConstants.PLATFORM_TRANSACTIONS + "/"))
                || (path.equals(BffConstants.TENANT_TRANSACTIONS)
                        || path.startsWith(BffConstants.TENANT_TRANSACTIONS + "/"))
                || path.equals(BffConstants.CSRF) || path.equals(BffConstants.PLATFORM_LOGIN)
                || path.equals(BffConstants.TENANT_LOGIN) || path.equals(BffConstants.TENANT_SELECT)
                || path.equals(BffConstants.ME) || path.equals(BffConstants.LOGOUT);
    }

    private Mono<Void> error(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        BaseErrorCode code = status == HttpStatus.UNAUTHORIZED ? BaseErrorCode.UNAUTHORIZED
                : status == HttpStatus.FORBIDDEN ? BaseErrorCode.FORBIDDEN : BaseErrorCode.REQUEST_FALLBACK;
        try {
            return exchange.getResponse()
                .writeWith(Mono
                    .just(exchange.getResponse().bufferFactory().wrap(mapper.writeValueAsBytes(R.error(code)))));
        }
        catch (Exception exception) {
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return GatewayFilterOrders.SESSION_RELAY;
    }

    /** 单次源状态读取的决定，不捕获下游业务异常。 */
    private record Decision(BffSession session, HttpStatus error) {
    }

}
