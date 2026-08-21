package com.ingot.framework.commons.constants;

/**
 * <p>JWT claim 名的跨模块单一事实来源，供签发侧、资源服务器与网关共用。</p>
 *
 * <p>标准 RFC 7519 claim（{@code iss}/{@code sub}/{@code jti} 等）与本接口并列维护，
 * 避免 Gateway 为读取自定义 claim 去依赖 security-common 的 OAuth2/JWT 栈。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public interface InJwtClaimNames {

    /**
     * JWT ID（RFC 7519 {@code jti}）。每次签发都不同，仅标识单个 Access Token。
     */
    String JTI = "jti";

    /**
     * 会话 ID，值等于 {@code OAuth2Authorization.id}。
     *
     * <p>refresh 换发不改变该值，因此它是会话主键：在线态查询、强制下线、并发会话策略
     * 一律以 sid 为准。缺失该 claim 的 Token 视为无效。</p>
     */
    String SID = "sid";

    /**
     * 用户 ID。
     */
    String ID = "i";

    /**
     * 租户 ID。
     */
    String TENANT = "org";

    /**
     * 认证类型。
     */
    String AUTH_TYPE = "tat";

    /**
     * 用户类型。瘦身 JWT 可能省略该 claim，改由 Redis OnlineToken 补全。
     */
    String USER_TYPE = "ut";
}
