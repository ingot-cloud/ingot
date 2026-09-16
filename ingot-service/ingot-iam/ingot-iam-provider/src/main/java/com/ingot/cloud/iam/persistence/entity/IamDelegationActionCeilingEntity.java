package com.ingot.cloud.iam.persistence.entity;

import java.math.BigInteger;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>保存委派派生授权的操作与范围上限。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@TableName(value = "iam_delegation_action_ceiling", autoResultMap = true)
public class IamDelegationActionCeilingEntity {
    /** 委派 ID。 */
    @TableField("delegation_id")
    private BigInteger delegationId;

    /** 操作 ID。 */
    @TableField("action_id")
    private BigInteger actionId;

    /** 范围表达式 JSON 数组。 */
    @TableField("scopes")
    private String scopes;

    /** 范围参数绑定 JSON 对象。 */
    @TableField("scope_bindings")
    private String scopeBindings;

}
