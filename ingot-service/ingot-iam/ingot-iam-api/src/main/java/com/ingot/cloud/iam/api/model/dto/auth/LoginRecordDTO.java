package com.ingot.cloud.iam.api.model.dto.auth;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 登录记录 DTO（Auth 服务通知 IAM/Member 记录登录事件）
 *
 * @author jymot
 * @since 2026-02-13
 */
@Data
public class LoginRecordDTO implements Serializable {

    /**
     * 是否登录成功
     */
    private boolean success;

    /**
     * 用户ID（登录成功时必填；失败时若能查到则填）
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 客户端IP
     */
    private String clientIp;

    /**
     * 用户类型
     */
    private String userType;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 登录时间
     */
    private LocalDateTime loginAt;

    /**
     * 失败原因码（登录失败时填写）
     */
    private String failureReason;
}
