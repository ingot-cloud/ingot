package com.ingot.cloud.iam.api.model.dto.user;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * 手动锁定账号请求参数
 *
 * @author jymot
 * @since 2026-02-13
 */
@Data
public class AccountLockDTO implements Serializable {

    /**
     * 锁定原因描述（必填）
     */
    private String reasonDetail;

    /**
     * 锁定到期时间（null 表示永久锁定）
     */
    private LocalDateTime lockedUntil;
}
