package com.ingot.cloud.iam.infrastructure.persistence.entity;

import com.ingot.framework.commons.model.iam.RoleKind;

import java.math.BigInteger;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ingot.framework.commons.model.iam.*;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>保存委派可派生的角色版本。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@TableName(value = "iam_delegation_role_revision", autoResultMap = true)
public class IamDelegationRoleRevisionEntity {
    /** 委派 ID。 */
    @TableField("delegation_id")
    private BigInteger delegationId;

    /** 角色版本 ID。 */
    @TableField("revision_id")
    private BigInteger revisionId;

    /** 角色版本种类。 */
    @TableField("revision_kind")
    private RoleKind revisionKind;

}
