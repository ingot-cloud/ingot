package com.ingot.cloud.iam.persistence.entity;

import java.math.BigInteger;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.ingot.framework.commons.model.iam.MemberStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>保存租户成员资料和资格，不修改全局凭证。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@TableName(value = "iam_tenant_member", autoResultMap = true)
public class IamTenantMemberEntity {
    /** 租户成员 ID。 */
    @TableId(value = "id", type = IdType.INPUT)
    private BigInteger id;

    /** 所属租户 ID。 */
    @TableField("tenant_id")
    private BigInteger tenantId;

    /** 全局账号 ID。 */
    @TableField("account_id")
    private BigInteger accountId;

    /** 组织显示名称。 */
    @TableField("display_name")
    private String displayName;

    /** 组织头像，可空。 */
    @TableField("avatar")
    private String avatar;

    /** 组织通讯录手机号，可空。 */
    @TableField("phone")
    private String phone;

    /** 组织通讯录邮箱，可空。 */
    @TableField("email")
    private String email;

    /** 当前租户成员资格。 */
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
