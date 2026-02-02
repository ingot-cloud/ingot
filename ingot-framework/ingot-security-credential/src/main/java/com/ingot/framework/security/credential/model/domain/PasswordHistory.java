package com.ingot.framework.security.credential.model.domain;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * 密码历史记录（通用模型）
 * <p>采用环形缓冲设计，固定记录数量</p>
 *
 * @author jymot
 * @since 2026-01-23
 */
@Data
public class PasswordHistory implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 密码哈希值
     */
    private String passwordHash;

    /**
     * 序号（用于环形缓冲，从1开始）
     */
    private Integer sequenceNumber;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
