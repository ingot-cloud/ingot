package com.ingot.cloud.gateway.security;

import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.gateway.rule.client.blacklist.BlacklistService;
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
 * 黑白名单过滤器。
 *
 * <p>顺序：{@link SecurityPolicyFilterOrder#BLACKLIST}，在 {@link com.ingot.cloud.gateway.filter.auth.IdentityResolveFilter} 之后、
 * {@link ChallengeFilter} 与 Sentinel 限流之前。</p>
 *
 * <h3>处理逻辑</h3>
 * <ol>
 *     <li>读取 {@link ClientIdentity}；缺失时跳过（不应发生在正常链路）</li>
 *     <li><b>白名单优先</b>：命中静态白名单 → 写入 {@link #ATTR_WHITELISTED}，
 *         后续 {@link ChallengeFilter} 与 {@link WhitelistAwareSentinelGatewayFilter} 会跳过</li>
 *     <li>检查 Redis 临时封禁（限流违规自动封禁产物，见 {@link TempBlockStore}）</li>
 *     <li>检查 SDK 静态黑名单（IP / CIDR / 设备 / 用户 / UA / Referer）</li>
 *     <li>命中 → <b>403</b> + {@code FORBIDDEN_BLOCKED}</li>
 * </ol>
 *
 * <p>临时封禁 Key：{@code in:gw:bl:tmp:{keyType}:{keyValue}}，当前检查 IP 与 DEVICE 两个维度。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BlacklistFilter implements GlobalFilter, Ordered {

    /**
     * 白名单标记。值为 {@code true} 时表示当前请求命中静态白名单，
     * 后续挑战与 Sentinel 限流应跳过。
     */
    public static final String ATTR_WHITELISTED = "ingot.security.whitelisted";

    private static final String BLOCKED_CODE = "FORBIDDEN_BLOCKED";

    private final ObjectProvider<BlacklistService> blacklistProvider;
    private final TempBlockStore tempBlockStore;
    private final ReactiveResponseWriter responseWriter;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ClientIdentity identity = (ClientIdentity) exchange.getAttributes().get(ClientIdentity.ATTR_KEY);
        if (identity == null) {
            return chain.filter(exchange);
        }
        BlacklistService bl = blacklistProvider.getIfAvailable();

        if (bl != null && bl.isWhitelisted(identity.getIp(), identity.getDevice(),
                identity.getUserId(), identity.getUserAgent(), identity.getReferer())) {
            exchange.getAttributes().put(ATTR_WHITELISTED, Boolean.TRUE);
            return chain.filter(exchange);
        }

        Mono<Boolean> tempBlockedIp = tempBlockStore.isBlocked("IP", identity.getIp());
        Mono<Boolean> tempBlockedDevice = identity.getDevice() == null
                ? Mono.just(false)
                : tempBlockStore.isBlocked("DEVICE", identity.getDevice());

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
                                R.error(BLOCKED_CODE, "Request blocked"));
                    }
                    return chain.filter(exchange);
                });
    }

    @Override
    public int getOrder() {
        return SecurityPolicyFilterOrder.BLACKLIST;
    }
}
