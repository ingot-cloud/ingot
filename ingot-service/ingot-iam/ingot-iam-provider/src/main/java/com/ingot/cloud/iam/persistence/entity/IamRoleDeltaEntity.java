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
 * <p>保存租户角色版本相对共享基础的单操作差异。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@TableName(value = "iam_role_delta", autoResultMap = true)
public class IamRoleDeltaEntity {
    /** 角色版本 ID。 */
    @TableField("revision_id")
    private BigInteger revisionId;

    /** 操作 ID。 */
    @TableField("action_id")
    private BigInteger actionId;

    /** 差异操作。 */
    @TableField("operation")
    private RoleDeltaOperation operation;

    /** 范围表达式 JSON 数组。 */
    @TableField("scopes")
    private String scopes;

}
