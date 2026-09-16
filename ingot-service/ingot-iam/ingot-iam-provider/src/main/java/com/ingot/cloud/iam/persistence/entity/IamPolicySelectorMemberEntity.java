package com.ingot.cloud.iam.persistence.entity;

import java.math.BigInteger;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>保存策略选择器中的成员引用，所有访问必须限定租户。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@TableName(value = "iam_policy_selector_member", autoResultMap = true)
public class IamPolicySelectorMemberEntity {
    /** 所属租户 ID。 */
    @TableField("tenant_id")
    private BigInteger tenantId;

    /** 选择器 ID。 */
    @TableField("selector_id")
    private BigInteger selectorId;

    /** 租户成员 ID。 */
    @TableField("member_id")
    private BigInteger memberId;
}
