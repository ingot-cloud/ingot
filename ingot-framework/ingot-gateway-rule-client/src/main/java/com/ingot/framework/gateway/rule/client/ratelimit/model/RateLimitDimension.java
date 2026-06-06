package com.ingot.framework.gateway.rule.client.ratelimit.model;

import java.util.Locale;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 限流维度。
 *
 * <p>每个维度对应 Sentinel {@code GatewayParamFlowItem.fieldName} 取值来源：</p>
 * <ul>
 *     <li>{@link #IP}（DB 短码 {@code IP}）：客户端真实 IP，从
 *         {@code X-Client-Real-IP} Header 读取（由
 *         {@code RequestGlobalFilter} 在最前面标准化写入）。</li>
 *     <li>{@link #DEVICE}（DB 短码 {@code DV}）：设备指纹，从
 *         {@code X-In-Ca-Sig} Header 读取（来源：BFF）。</li>
 *     <li>{@link #USER}（DB 短码 {@code UI}）：用户 ID，从
 *         {@code X-User-Id} Header 读取（由网关 IdentityResolveFilter 从 JWT attribute 回填）。
 *         匿名请求 userId 为空时，Sentinel 取不到参数会退化为按 API 整体限流。</li>
 * </ul>
 *
 * <p>DB 表 {@code gateway_rate_limit_rule.dimension} 字段为 {@code char(2)} 短码，
 * yaml 配置（local 模式）习惯写枚举全名；本类同时支持两种写法，由
 * {@link #fromCode(String)} 统一解析。</p>
 *
 * <h3>yaml 示例</h3>
 * <pre>{@code
 * dimension: IP      # 或 DEVICE / USER；remote 模式 DB 短码 IP/DV/UI 亦可
 * }</pre>
 *
 * @author jy
 * @since 2026/5/26
 */
@Getter
@RequiredArgsConstructor
@Accessors(fluent = true)
public enum RateLimitDimension {

    /** 按客户端真实 IP 限流；Header {@code X-Client-Real-IP}；DB 短码 {@code IP}。 */
    IP("IP"),

    /** 按设备指纹限流；Header {@code X-In-Ca-Sig}；DB 短码 {@code DV}。 */
    DEVICE("DV"),

    /** 按用户 ID 限流；Header {@code X-User-Id}；匿名时退化为 API 整体限流；DB 短码 {@code UI}。 */
    USER("UI");

    private final String dbCode;

    /**
     * 解析维度字符串，同时兼容 DB 短码（IP/DV/UI）与枚举全名（IP/DEVICE/USER）。
     * <p>大小写不敏感；为空、未知值时返回 {@link #IP} 作为最稳妥的默认。</p>
     */
    public static RateLimitDimension fromCode(String raw) {
        if (raw == null) {
            return IP;
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return IP;
        }
        String upper = s.toUpperCase(Locale.ROOT);
        for (RateLimitDimension d : values()) {
            if (d.dbCode.equals(upper) || d.name().equals(upper)) {
                return d;
            }
        }
        return IP;
    }
}
