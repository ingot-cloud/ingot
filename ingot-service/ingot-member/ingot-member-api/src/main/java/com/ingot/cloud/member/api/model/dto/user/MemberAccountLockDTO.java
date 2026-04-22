package com.ingot.cloud.member.api.model.dto.user;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * 会员账号手动锁定请求参数
 *
 * @author jymot
 * @since 2026-02-14
 */
@Data
public class MemberAccountLockDTO implements Serializable {

    /**
     * 锁定原因描述（必填）
     */
    private String reasonDetail;

    /**
     * 锁定到期时间（null 表示永久锁定）
     */
    private LocalDateTime lockedUntil;
}
