package com.ingot.cloud.iam.persistence.entity;

import java.math.BigInteger;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ingot.framework.commons.model.iam.*;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>保存委派接收成员，不写入数据库生成列。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@TableName(value = "iam_delegation_recipient_member", autoResultMap = true)
public class IamDelegationRecipientMemberEntity {
    /** 委派 ID。 */
    @TableField("delegation_id")
    private BigInteger delegationId;

    /** 授权域。 */
    @TableField("domain")
    private AuthorizationDomain domain;

    /** 所属租户 ID，平台域为空。 */
    @TableField("tenant_id")
    private BigInteger tenantId;

    /** 平台成员 ID。 */
    @TableField("platform_member_id")
    private BigInteger platformMemberId;

    /** 租户成员 ID。 */
    @TableField("tenant_member_id")
    private BigInteger tenantMemberId;

}
