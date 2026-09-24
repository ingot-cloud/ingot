package com.ingot.cloud.iam.persistence.entity;

import java.math.BigInteger;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.ingot.framework.commons.model.iam.MemberStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>保存租户元数据及本租户所有者引用。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@TableName(value = "iam_tenant", autoResultMap = true)
public class IamTenantEntity {
    /** 租户 ID。 */
    @TableId(value = "id", type = IdType.INPUT)
    private BigInteger id;

    /** 组织名称。 */
    @TableField("name")
    private String name;

    /** 组织头像，可空。 */
    @TableField("avatar")
    private String avatar;

    /** 本租户所有者成员 ID。 */
    @TableField("owner_member_id")
    private BigInteger ownerMemberId;

    /** 最近一次提交的套餐 ID，可空。 */
    @TableField("plan_id")
    private BigInteger planId;

    /** 组织启用状态。 */
    @TableField("enabled")
    private Boolean enabled;

    /** 递增配置版本。 */
    @TableField("version")
    private BigInteger version;

    /** 创建时间，UTC。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 更新时间，UTC。 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /** 软删除时间，UTC。 */
    @TableField("deleted_at")
    private LocalDateTime deletedAt;

}
