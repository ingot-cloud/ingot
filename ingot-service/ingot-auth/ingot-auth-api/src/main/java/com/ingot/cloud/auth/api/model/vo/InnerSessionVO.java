package com.ingot.cloud.auth.api.model.vo;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>在线会话视图，Auth Inner 会话查询接口的统一出参。</p>
 *
 * <p>只暴露审计与管理面需要的会话事实，不携带资源服务器补全鉴权用的
 * authorities 与 deptIds —— 那些属于 Auth 内部的会话主数据，跨服务传输既无消费方也放大泄露面。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InnerSessionVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 会话 ID，撤销接口的唯一入参。
     */
    private String sid;

    /**
     * 当前 Access Token 的 jti，会话续期后会变化。
     */
    private String jti;

    private Long userId;

    private Long tenantId;

    /**
     * 登录账号名。
     */
    private String principalName;

    private String clientId;

    /**
     * 并发会话策略，取值见 {@link com.ingot.framework.commons.model.security.TokenAuthTypeEnum}。
     */
    private String authType;

    /**
     * 用户类型，取值见 {@link com.ingot.framework.commons.model.security.UserTypeEnum}。
     */
    private String userType;

    private String ipAddress;

    private String userAgent;

    private String deviceType;

    private String os;

    private String browser;

    /**
     * 登录地理位置，未解析时为空。
     */
    private String location;

    /**
     * 会话创建时间，续期不改写。
     */
    private Instant issuedAt;

    /**
     * 会话过期时间，对齐 Refresh Token 剩余寿命。
     */
    private Instant expiresAt;

    /**
     * 最近一次凭据活动时间（登录或续期），非每请求刷新。
     */
    private Instant lastAccessAt;
}
