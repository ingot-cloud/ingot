package com.ingot.framework.commons.model.bff;

import java.io.Serial;
import java.io.Serializable;

import lombok.Data;

/**
 * <p>
 * BFF 私有正式会话，在应用隔离的 Redis v2 中供网关验证和中继。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
@Data
@lombok.ToString(onlyExplicitlyIncluded = true)
public class BffSession implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 仅服务端持有的 Access Token。 */
    private String accessToken;

    /** 仅服务端持有的 Refresh Token。 */
    private String refreshToken;

    /** 正式会话 UTC epoch seconds 截止，不被请求滑动续期。 */
    private long expiresAt;

    /** 租户身份；平台为 null。 */
    private String tenantId;

    /** 已认证用户 ID。 */
    private Long userId;

    /** 固定内部 OAuth 客户端。 */
    private String clientId;

    /** 可信内部应用。 */
    private String appId;

    /** 平台或租户授权域协议值。 */
    private String domain;

    /** 会话提交 UTC epoch seconds。 */
    private long createdAt;

    /**
     * Auth 侧会话 ID（JWT 的 {@code sid} 声明），登出时据此请求 Auth 撤销会话。
     * <p>
     * 取自 Access Token 声明而非另建索引：Access Token 过期后仍可凭它撤销整条凭据链。
     * </p>
     */
    private String sid;

    /**
     * 客户端指纹，用于防止 Cookie 被盗用。
     */
    private String fingerprint;

    /**
     * Auth 服务的会话 Cookie（JSESSIONID），仅供服务端撤销关联上下文。 不下发给浏览器，不出现在日志或公开接口中。
     */
    private String authCookie;

    /** 新生成的正式会话 ID，仅服务端存储，不进入 API。 */
    private String sessionId;

    /** 首次 Cookie 交付/恢复的绝对截止秒数，确认交付后不限制原会话寿命。 */
    private long recoveryExpiresAt;

    /** 创建本次结果的匿名浏览器绑定。 */
    private String bindingId;

    /** 原始登录事务，供交付确认和同源恢复关联。 */
    private String transactionId;

    /** 指纹来源；关闭指纹时为 null。 */
    private BffFingerprintMode fingerprintMode;

}
