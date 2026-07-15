package com.ingot.framework.commons.constants;

/**
 * <p>HTTP 请求头常量，统一维护标准/通用头与 Ingot 自定义头。</p>
 *
 * <p>标准头由代理、负载均衡等基础设施写入；自定义头由平台内部约定，
 * 其中 {@link #GATEWAY_INTERNAL_HEADERS} 在网关入口由 {@code RequestGlobalFilter} 统一剥离，
 * 后续 Filter 按需写入可信值。</p>
 *
 * @author wangchao
 * @since 1.0.0
 */
public interface HeaderConstants {

    // ── 标准 / 通用 HTTP Header（代理、负载均衡等）──

    /**
     * 多级代理转发的客户端 IP 链。
     */
    String X_FORWARDED_FOR = "X-Forwarded-For";

    /**
     * Nginx 等反向代理常用的客户端真实 IP。
     */
    String X_REAL_IP = "X-Real-IP";

    /**
     * WebLogic 代理客户端 IP。
     */
    String PROXY_CLIENT_IP = "Proxy-Client-IP";

    /**
     * WebLogic 代理客户端 IP（WL 前缀变体）。
     */
    String WL_PROXY_CLIENT_IP = "WL-Proxy-Client-IP";

    /**
     * 历史兼容：部分代理使用的客户端 IP 头。
     */
    String HTTP_CLIENT_IP = "HTTP_CLIENT_IP";

    /**
     * 历史兼容：部分代理使用的 X-Forwarded-For 变体。
     */
    String HTTP_X_FORWARDED_FOR = "HTTP_X_FORWARDED_FOR";

    /**
     * 网关入口：从原始代理头解析客户端 IP（不含 {@link #INNER_CLIENT_REAL_IP} 与历史兼容头）。
     */
    String[] PROXY_IP_HEADERS = {
            X_FORWARDED_FOR,
            X_REAL_IP,
            PROXY_CLIENT_IP,
            WL_PROXY_CLIENT_IP,
    };

    // ── Ingot 自定义 Header（平台内部约定）──

    /**
     * 租户
     */
    String TENANT = "Tenant";

    /**
     * 请求来源，内部 Header 字段
     */
    String SECURITY_FROM = "In-Inner-From";

    /**
     * BFF 设备指纹 Header 名称。
     * 使用非语义化名称，避免暴露用途。
     */
    String BFF_DEVICE_FINGERPRINT_HEADER = "In-Ca-Sig";

    /**
     * 网关解析并标准化后的客户端真实 IP（内部 Header，不对外暴露）。
     * 由 RequestGlobalFilter 在网关入口统一设置，下游服务和网关过滤器都从此 Header 读取，
     * 确保同一请求在任何位置获取到的客户端 IP 一致。
     */
    String INNER_CLIENT_REAL_IP = "In-Inner-Client-Real-IP";

    /**
     * 当前请求用户 ID（网关内部 Header）。
     * <p>由网关 IdentityResolveFilter 从 exchange attribute 回填，供 Sentinel USER 维度限流；
     * 外部传入会在 RequestGlobalFilter 入口剥离。</p>
     */
    String INNER_USER_ID = "In-Inner-User-Id";

    /**
     * 应用层：解析请求来源 IP（网关标准化头优先，含历史兼容头）。
     */
    String[] REQUEST_SOURCE_IP_HEADERS = {
            INNER_CLIENT_REAL_IP,
            X_FORWARDED_FOR,
            X_REAL_IP,
            PROXY_CLIENT_IP,
            WL_PROXY_CLIENT_IP,
            HTTP_CLIENT_IP,
            HTTP_X_FORWARDED_FOR,
    };

    /**
     * 仅允许网关链路写入的内部 Header；RequestGlobalFilter 入口统一 remove，
     * 后续 Filter 按需 set。
     */
    String[] GATEWAY_INTERNAL_HEADERS = {
            SECURITY_FROM,
            INNER_CLIENT_REAL_IP,
            INNER_USER_ID,
    };
}
