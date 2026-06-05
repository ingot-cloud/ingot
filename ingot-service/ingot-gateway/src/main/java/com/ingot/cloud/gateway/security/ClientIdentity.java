package com.ingot.cloud.gateway.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 客户端身份上下文，供黑白名单 / 限流 / 挑战等安全 Filter 复用。
 *
 * <p>由 {@link com.ingot.cloud.gateway.filter.auth.IdentityResolveFilter} 在请求最前面填充并以 {@link #ATTR_KEY} 存入
 * {@code ServerWebExchange.attributes}，避免各 Filter 重复解析 Header / JWT。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Getter
@Builder
@AllArgsConstructor
public class ClientIdentity {

    public static final String ATTR_KEY = "ingot.security.identity";

    /** 客户端真实 IP，来自 {@code X-Client-Real-IP}（{@link com.ingot.cloud.gateway.filter.RequestGlobalFilter} 标准化）。 */
    private final String ip;

    /** 设备指纹，来自 {@code X-In-Ca-Sig}。 */
    private final String device;

    /**
     * 用户 ID，来自 {@link com.ingot.cloud.gateway.filter.auth.AuthContextRelayFilter} 解析 JWT 后
     * 写入的 exchange attribute，由本 Filter 聚合进 {@link ClientIdentity}。
     */
    private final String userId;

    private final String userAgent;

    /** Referer 请求头，供 {@code RF} 维度黑白名单正则匹配。 */
    private final String referer;
}
