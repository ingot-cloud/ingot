package com.ingot.cloud.iam.persistence.entity;

import java.math.BigInteger;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.ingot.framework.commons.model.iam.MemberStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>维护保留的账号安全锁定状态。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@TableName(value = "account_lock_state", autoResultMap = true)
public class IamAccountLockStateEntity {
    /** 锁定记录 ID。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 全局账号 ID。 */
    @TableField("user_id")
    private Long userId;

    /** 既有用户体系编码，取 UserTypeEnum.value。 */
    @TableField("user_type")
    private String userType;

    /** 账号是否锁定。 */
    @TableField("locked")
    private Boolean locked;

    /** 最近更新时间，使用 UTC。 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

}
