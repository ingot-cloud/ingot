package com.ingot.cloud.iam.persistence.entity;

import java.math.BigInteger;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>保存应用人群中的成员引用。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@TableName(value = "iam_audience_member", autoResultMap = true)
public class IamAudienceMemberEntity {
    /** 所属租户 ID。 */
    @TableField("tenant_id")
    private BigInteger tenantId;

    /** 所属应用 ID。 */
    @TableField("application_id")
    private BigInteger applicationId;

    /** 租户成员 ID。 */
    @TableField("member_id")
    private BigInteger memberId;

}
