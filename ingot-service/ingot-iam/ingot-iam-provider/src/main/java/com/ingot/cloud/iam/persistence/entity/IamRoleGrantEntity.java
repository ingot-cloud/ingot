package com.ingot.cloud.iam.persistence.entity;

import java.math.BigInteger;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.ingot.framework.commons.model.iam.*;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>映射角色版本授权的持久化字段，不写入数据库生成列。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@TableName(value = "iam_role_grant", autoResultMap = true)
public class IamRoleGrantEntity {
    /** 角色版本 ID。 */
    @TableField("revision_id")
    private BigInteger revisionId;

    /** 操作 ID。 */
    @TableField("action_id")
    private BigInteger actionId;

    /** 范围表达式 JSON 数组。 */
    @TableField("scopes")
    private String scopes;

}
