package com.ingot.framework.commons.constants;

/**
 * <p>Description  : Header扩展常量.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-12-24.</p>
 * <p>Time         : 16:54.</p>
 */
public interface HeaderConstants {

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
    String BFF_DEVICE_FINGERPRINT_HEADER = "X-In-Ca-Sig";

    /**
     * 网关解析并标准化后的客户端真实 IP（内部 Header，不对外暴露）。
     * 由 RequestGlobalFilter 在网关入口统一设置，下游服务和网关过滤器都从此 Header 读取，
     * 确保同一请求在任何位置获取到的客户端 IP 一致。
     */
    String CLIENT_REAL_IP = "X-Client-Real-IP";

    /**
     * 当前请求用户 ID（网关内部 Header）。
     * <p>由网关 IdentityResolveFilter 从 exchange attribute 回填，供 Sentinel USER 维度限流；
     * 外部传入会在 RequestGlobalFilter 入口剥离。</p>
     */
    String X_USER_ID = "X-User-Id";

    /**
     * 仅允许网关链路写入的内部 Header；RequestGlobalFilter 入口统一 remove，
     * 后续 Filter 按需 set。
     */
    String[] GATEWAY_INTERNAL_HEADERS = {
            SECURITY_FROM,
            CLIENT_REAL_IP,
            X_USER_ID,
    };
}
