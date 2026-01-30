package com.ingot.framework.security.credential.model.domain;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 密码过期信息（通用模型）
 *
 * @author jymot
 * @since 2026-01-23
 */
@Data
public class PasswordExpiration implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 用户ID（唯一）
     */
    private Long userId;

    /**
     * 最后修改密码时间
     */
    private LocalDateTime lastChangedAt;

    /**
     * 密码过期时间
     */
    private LocalDateTime expiresAt;

    /**
     * 是否强制修改
     */
    private Boolean forceChange;

    /**
     * 剩余宽限登录次数
     */
    private Integer graceLoginRemaining;

    /**
     * 下次提醒时间
     */
    private LocalDateTime nextWarningAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
