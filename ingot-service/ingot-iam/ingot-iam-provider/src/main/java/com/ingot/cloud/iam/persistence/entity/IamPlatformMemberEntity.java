package com.ingot.cloud.iam.persistence.entity;

import java.math.BigInteger;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.ingot.framework.commons.model.iam.MemberStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>保存独立的平台成员资格，不包含租户部门。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@TableName(value = "iam_platform_member", autoResultMap = true)
public class IamPlatformMemberEntity {
    /** 平台成员 ID。 */
    @TableId(value = "id", type = IdType.INPUT)
    private BigInteger id;

    /** 关联全局账号 ID。 */
    @TableField("account_id")
    private BigInteger accountId;

    /** 平台显示名称。 */
    @TableField("display_name")
    private String displayName;

    /** 平台头像，可空。 */
    @TableField("avatar")
    private String avatar;

    /** 平台成员资格。 */
    @TableField("status")
    private MemberStatus status;

    /** 递增成员版本。 */
    @TableField("version")
    private BigInteger version;

    /** 创建时间，UTC。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 更新时间，UTC。 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

}
