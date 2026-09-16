package com.ingot.framework.security.account.adapter.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 锁定状态实体（基础类，IAM和Member可继承）
 *
 * @author jymot
 * @since 2026-02-13
 */
@Data
@TableName("account_lock_state")
public class AccountLockStateEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户类型（存 {@code UserTypeEnum.value}：0-系统用户 1-C端用户）
     */
    private String userType;

    /**
     * 是否锁定
     */
    private Boolean locked;

    /**
     * 锁定类型（MANUAL/AUTO）
     */
    private String lockType;

    /**
     * 锁定原因代码
     */
    private String lockReasonCode;

    /**
     * 锁定原因详情
     */
    private String lockReasonDetail;

    /**
     * 锁定时间
     */
    private LocalDateTime lockedAt;

    /**
     * 锁定到期时间
     */
    private LocalDateTime lockedUntil;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 连续失败次数
     */
    private Integer failedLoginCount;

    /**
     * 最后失败时间
     */
    private LocalDateTime lastFailedAt;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
