package com.ingot.cloud.gateway.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 客户端身份上下文，供黑白名单 / 限流 / 挑战等安全 Filter 按维度（IP / DEVICE / USER / RF）复用。
 *
 * <p>由 {@link com.ingot.cloud.gateway.filter.auth.IdentityResolveFilter} 聚合 Header 与 JWT 解析结果，
 * 以 {@link #ATTR_KEY}（与 {@link GatewaySecurityConstants#ATTR_CLIENT_IDENTITY} 等价）写入
 * {@code ServerWebExchange.attributes}，避免各 Filter 重复解析。</p>
 *
 * <h3>数据来源</h3>
 * <ul>
 *     <li>IP / 设备 / UA / Referer：{@link com.ingot.cloud.gateway.filter.RequestGlobalFilter} 标准化后的 Header</li>
 *     <li>userId：{@link com.ingot.cloud.gateway.filter.auth.AuthContextRelayFilter} 从 Bearer JWT payload 解析</li>
 * </ul>
 *
 * <h3>下游读取示例</h3>
 * <pre>{@code
 * ClientIdentity id = exchange.getAttribute(GatewaySecurityConstants.ATTR_CLIENT_IDENTITY);
 * String ip = id != null ? id.getIp() : null;
 * }</pre>
 *
 * @author jy
 * @since 2026/5/26
 */
@Getter
@Builder
@AllArgsConstructor
public class ClientIdentity {

    /** Exchange attribute 键；与 {@link GatewaySecurityConstants#ATTR_CLIENT_IDENTITY} 相同。 */
    public static final String ATTR_KEY = GatewaySecurityConstants.ATTR_CLIENT_IDENTITY;

    /**
     * 客户端真实 IP。
     * <p>来源：{@link com.ingot.framework.commons.constants.HeaderConstants#INNER_CLIENT_REAL_IP}，
     * 由 {@link com.ingot.cloud.gateway.filter.RequestGlobalFilter} 按代理头优先级解析并标准化。
     * 供黑白名单 {@code IP} 维度、Sentinel 限流 {@code IP} 资源名使用。</p>
     */
    private final String ip;

    /**
     * 设备指纹。
     * <p>来源：{@link com.ingot.framework.commons.constants.HeaderConstants#BFF_DEVICE_FINGERPRINT_HEADER}
     *（{@code In-Ca-Sig}）。供黑白名单 {@code DEVICE} 维度匹配。</p>
     */
    private final String device;

    /**
     * 用户 ID。
     * <p>来源：{@link com.ingot.cloud.gateway.filter.auth.AuthContextRelayFilter} 解析 JWT claim
     * {@code i} 后写入 {@link com.ingot.cloud.gateway.filter.auth.AuthContextAttributes#USER_ID}，
     * 再由 {@link com.ingot.cloud.gateway.filter.auth.IdentityResolveFilter} 聚合进本对象；
     * 非空时同步回填 {@code In-Inner-User-Id} Header 供 Sentinel {@code USER} 维度。</p>
     */
    private final String userId;

    /**
     * User-Agent 原始值。
     * <p>来源：标准 {@code User-Agent} 请求头；当前主要用于审计与日志，黑白名单维度未直接使用。</p>
     */
    private final String userAgent;

    /**
     * Referer 请求头。
     * <p>来源：标准 {@code Referer} 请求头；供黑白名单 {@code RF} 维度正则匹配。</p>
     */
    private final String referer;
}
