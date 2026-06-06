package com.ingot.cloud.gateway.security;

import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.gateway.rule.client.blacklist.BlacklistService;
import com.ingot.framework.gateway.rule.client.blacklist.model.IpKeyType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 黑白名单过滤器：静态名单 + Redis 临时封禁，命中返回 HTTP 403。
 *
 * <p>执行顺序 {@link SecurityPolicyFilterOrder#BLACKLIST}，位于
 * {@link com.ingot.cloud.gateway.filter.auth.IdentityResolveFilter} 之后、
 * {@link ChallengeFilter} 与 Sentinel 限流之前。</p>
 *
 * <h3>处理逻辑</h3>
 * <ol>
 *     <li>读取 {@link ClientIdentity}；缺失时放行（异常链路兜底）</li>
 *     <li><b>白名单优先</b>：命中静态白名单 → 写入 {@link #ATTR_WHITELISTED}，
 *         后续挑战与 Sentinel 跳过</li>
 *     <li>检查 Redis 临时封禁（{@link TempBlockStore}，限流违规升级产物）</li>
 *     <li>检查 SDK 静态黑名单（IP / CIDR / 设备 / 用户 / UA / Referer）</li>
 *     <li>命中 → HTTP 403，{@code code={@link GatewaySecurityConstants#CODE_FORBIDDEN_BLOCKED}}</li>
 * </ol>
 *
 * <h3>相关配置</h3>
 * <pre>{@code
 * ingot:
 *   security:
 *     blacklist:
 *       enabled: true
 *       policy:
 *         mode: remote          # 或 local + items 内联
 *         items:
 *           - list-type: WHITE
 *             key-type: IP
 *             key-value: 10.0.0.0/8
 *             enabled: true
 * }</pre>
 *
 * @author jy
 * @since 2026/5/26
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BlacklistFilter implements GlobalFilter, Ordered {

    /**
     * 白名单标记 attribute 键。值为 {@code true} 时跳过后续挑战与 Sentinel 限流。
     *
     * @see GatewaySecurityConstants#ATTR_WHITELISTED
     */
    public static final String ATTR_WHITELISTED = GatewaySecurityConstants.ATTR_WHITELISTED;

    private final ObjectProvider<BlacklistService> blacklistProvider;
    private final TempBlockStore tempBlockStore;
    private final ReactiveResponseWriter responseWriter;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ClientIdentity identity = (ClientIdentity) exchange.getAttributes()
                .get(GatewaySecurityConstants.ATTR_CLIENT_IDENTITY);
        if (identity == null) {
            return chain.filter(exchange);
        }
        BlacklistService bl = blacklistProvider.getIfAvailable();

        if (bl != null && bl.isWhitelisted(identity.getIp(), identity.getDevice(),
                identity.getUserId(), identity.getUserAgent(), identity.getReferer())) {
            exchange.getAttributes().put(ATTR_WHITELISTED, Boolean.TRUE);
            return chain.filter(exchange);
        }

        Mono<Boolean> tempBlockedIp = tempBlockStore.isBlocked(IpKeyType.IP.dbCode(), identity.getIp());
        Mono<Boolean> tempBlockedDevice = identity.getDevice() == null
                ? Mono.just(false)
                : tempBlockStore.isBlocked(IpKeyType.DEVICE.dbCode(), identity.getDevice());

        return Mono.zip(tempBlockedIp, tempBlockedDevice)
                .flatMap(t -> {
                    boolean blockedByTemp = Boolean.TRUE.equals(t.getT1()) || Boolean.TRUE.equals(t.getT2());
                    boolean blockedByStatic = bl != null && bl.isBlocked(identity.getIp(),
                            identity.getDevice(), identity.getUserId(), identity.getUserAgent(),
                            identity.getReferer());
                    if (blockedByTemp || blockedByStatic) {
                        log.info("[BlacklistFilter] blocked ip={} device={} reason={}",
                                identity.getIp(), identity.getDevice(),
                                blockedByTemp ? "temp" : "static");
                        return responseWriter.writeJson(exchange.getResponse(), HttpStatus.FORBIDDEN,
                                R.error(GatewaySecurityConstants.CODE_FORBIDDEN_BLOCKED,
                                        GatewaySecurityConstants.MSG_REQUEST_BLOCKED));
                    }
                    return chain.filter(exchange);
                });
    }

    @Override
    public int getOrder() {
        return SecurityPolicyFilterOrder.BLACKLIST;
    }
}
