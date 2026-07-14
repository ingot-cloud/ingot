package com.ingot.cloud.gateway.filter;

import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.StrUtil;
import com.ingot.framework.commons.constants.HeaderConstants;
import com.ingot.framework.commons.utils.FingerprintUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 网关全局前置过滤器：剥离不可信内部 Header，并标准化客户端真实 IP。
 *
 * <p>作为安全策略链路的第一个 Filter（order = {@link GatewayFilterOrders#REQUEST_GLOBAL}），
 * 后续 {@link com.ingot.cloud.gateway.filter.auth.IdentityResolveFilter}、黑白名单、限流均依赖
 * 本 Filter 写入的 {@link HeaderConstants#CLIENT_REAL_IP}（{@code X-Client-Real-IP}）。</p>
 *
 * <h3>IP 解析优先级（从高到低）</h3>
 * <ol>
 *     <li>{@code X-Forwarded-For} — 取多级代理链最左侧可信 IP（{@code getMultistageReverseProxyIp}）</li>
 *     <li>{@code X-Real-IP}</li>
 *     <li>{@code Proxy-Client-IP}</li>
 *     <li>{@code WL-Proxy-Client-IP}</li>
 *     <li>{@code request.getRemoteAddress()} — 直连网关时的 socket 地址</li>
 * </ol>
 * <p>每个候选值经 {@link com.ingot.framework.commons.utils.FingerprintUtil#normalizeIp} 标准化；
 * 未知或空值跳过，全部缺失时写入空字符串。</p>
 *
 * <h3>Nginx 部署说明</h3>
 * <p>网关通常位于 Nginx 之后。须在 Nginx 配置中正确传递客户端 IP，例如：</p>
 * <pre>{@code
 * proxy_set_header X-Real-IP        $remote_addr;
 * proxy_set_header X-Forwarded-For  $proxy_add_x_forwarded_for;
 * }</pre>
 * <p>若未配置代理头，将回退到网关与 Nginx 之间的连接地址（多为 Nginx 内网 IP），
 * 导致黑白名单 / 限流的 IP 维度不准确。生产环境应确保仅信任一层反向代理写入的
 * {@code X-Forwarded-For}，避免客户端伪造。</p>
 *
 * <p>内部 Header 清单见 {@link HeaderConstants#GATEWAY_INTERNAL_HEADERS}；
 * 本 Filter 统一 remove，后续 Filter 只写入可信值。</p>
 */
@Slf4j
@Component
public class RequestGlobalFilter implements GlobalFilter, Ordered {
    private static final String[] IP_HEADERS = {
            "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"
    };

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("[Filter] - RequestGlobalFilter - path={}", exchange.getRequest().getPath());

        String clientIp = resolveClientIp(exchange.getRequest());

        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(httpHeaders -> {
                    for (String header : HeaderConstants.GATEWAY_INTERNAL_HEADERS) {
                        httpHeaders.remove(header);
                    }
                    httpHeaders.set(HeaderConstants.CLIENT_REAL_IP, clientIp);
                    log.info("[Filter] - RequestGlobalFilter - ip={}", clientIp);
                })
                .build();
        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        return GatewayFilterOrders.REQUEST_GLOBAL;
    }

    private String resolveClientIp(ServerHttpRequest request) {
        for (String header : IP_HEADERS) {
            String ip = request.getHeaders().getFirst(header);
            if (StrUtil.isNotEmpty(ip) && !NetUtil.isUnknown(ip)) {
                return FingerprintUtil.normalizeIp(NetUtil.getMultistageReverseProxyIp(ip));
            }
        }
        if (request.getRemoteAddress() != null) {
            return FingerprintUtil.normalizeIp(request.getRemoteAddress().getAddress().getHostAddress());
        }
        return "";
    }
}
