package com.ingot.cloud.security.api.model.vo.session;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

import lombok.Data;

/**
 * <p>安全中心在线会话视图，一条记录对应一个会话（sid），是管理面强制下线的操作对象。</p>
 *
 * <p>会话事实来自 Auth Inner，用户名 / 租户名由安全中心调 PMS 补全；PMS 不可用时
 * 名称字段为空，sid 级字段仍完整返回，保证管理员在 PMS 故障时依然能下线会话。
 * 不复用 Auth 的 {@code OnlineToken}，避免把授权服务器内部模型暴露到前端。</p>
 *
 * @author jy
 * @since 1.0.0
 * @apiNote 时间字段为 ISO-8601 UTC 瞬时（如 {@code 2026-08-19T03:21:00Z}），
 * 与库表 {@code yyyy-MM-dd HH:mm:ss} 的口径不同，前端需按时区转换后展示。
 */
@Data
public class PlatformSessionVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 会话 ID，按 sid 下线的唯一入参。
     */
    private String sid;

    /**
     * 当前 Access Token 的 jti，会话续期后会变化，仅用于与网关/审计日志对照。
     */
    private String jti;

    private Long userId;

    /**
     * 登录账号名，PMS 不可用时回落为会话内的 principalName。
     */
    private String username;

    /**
     * 用户昵称，PMS 不可用或非管理用户时为空。
     */
    private String nickname;

    /**
     * 用户头像，PMS 不可用或非管理用户时为空。
     */
    private String avatar;

    private Long tenantId;

    /**
     * 租户名称，PMS 不可用时为空。
     */
    private String tenantName;

    private String clientId;

    /**
     * 并发会话策略，取值见 {@code TokenAuthTypeEnum}：UNIQUE 单会话、STANDARD 多会话。
     */
    private String authType;

    /**
     * 用户类型，取值见 {@code UserTypeEnum}：0 管理用户、1 C 端用户。
     */
    private String userType;

    private String ipAddress;

    /**
     * 登录地理位置，未解析时为空。
     */
    private String location;

    private String deviceType;

    private String os;

    private String browser;

    private String userAgent;

    /**
     * 会话创建时间，续期不改写。
     */
    private Instant issuedAt;

    /**
     * 会话过期时间，对齐 Refresh Token 剩余寿命。
     */
    private Instant expiresAt;

    /**
     * 最近一次凭据活动时间（登录或续期），不随每次业务请求刷新。
     */
    private Instant lastAccessAt;
}
