package com.ingot.framework.commons.model.common;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 认证失败数据（用于跨服务登录失败事件传递）
 *
 * @author jymot
 * @since 2026-02-13
 */
@Data
public class AuthFailureDTO implements Serializable {
    /**
     * 登录账号（用户名 / 手机号等）
     */
    private String username;
    /**
     * 用户类型
     */
    private String userType;
    /**
     * 登录IP
     */
    private String ip;
    /**
     * 登录时间
     */
    private LocalDateTime time;
    /**
     * 失败原因码（OAuth2 错误码，如 invalid_grant）
     */
    private String errorCode;
    /**
     * 失败原因描述
     */
    private String errorMessage;
    /**
     * 租户ID（来自请求上下文）
     */
    private String tenantId;
    /**
     * OAuth2 client_id
     */
    private String clientId;
    /**
     * 设备指纹（In-Ca-Sig）
     */
    private String deviceId;
}
