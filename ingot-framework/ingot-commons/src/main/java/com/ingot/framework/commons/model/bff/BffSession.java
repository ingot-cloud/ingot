package com.ingot.framework.commons.model.bff;

import java.io.Serial;
import java.io.Serializable;

import lombok.Data;

/**
 * <p>BFF 会话数据模型，存储在 Redis 中供 BFF 服务和网关共同使用</p>
 *
 * <p>Redis key 格式为 {@code in:bff_session:{sessionId}}（通过
 * {@link com.ingot.framework.commons.constants.CacheConstants#bffSessionKey(String)} 构建）。
 * 登录阶段暂存 PKCE 参数，选租户完成后存储真正的 accessToken/refreshToken。
 * {@code fingerprint} 字段用于绑定客户端 IP+UA，防止 Cookie 被盗用后重放。</p>
 *
 * <p>此类放在 commons 层而非 BFF 服务内部，因为网关的 SessionTokenRelayFilter
 * 也需要反序列化该对象来提取 accessToken 和 fingerprint，避免使用魔法字符串。</p>
 *
 * @author jy
 * @see com.ingot.framework.commons.constants.CacheConstants#BFF_SESSION
 * @see com.ingot.framework.commons.utils.FingerprintUtil
 * @since 1.0.0
 */
@Data
public class BffSession implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String accessToken;
    private String refreshToken;
    private long expiresAt;
    private String tenantId;
    private Long userId;
    private String clientId;
    private long createdAt;

    /**
     * Auth 侧会话 ID（JWT 的 {@code sid} 声明），登出时据此请求 Auth 撤销会话。
     * <p>取自 Access Token 声明而非另建索引：Access Token 过期后仍可凭它撤销整条凭据链。</p>
     */
    private String sid;

    /**
     * 客户端指纹，用于防止 Cookie 被盗用。
     */
    private String fingerprint;

    /**
     * Auth 服务的会话 Cookie（JSESSIONID），仅在预授权→选租户之间暂存。
     * 用于 BFF 调用 authorize 时转发给 Auth 服务以恢复 SecurityContext。
     * 选租户完成后该字段不再使用。
     */
    private String authCookie;
}
