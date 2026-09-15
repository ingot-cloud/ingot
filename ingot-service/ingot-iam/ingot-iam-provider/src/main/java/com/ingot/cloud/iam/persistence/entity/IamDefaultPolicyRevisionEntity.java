package com.ingot.cloud.iam.persistence.entity;

import java.math.BigInteger;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.ingot.framework.commons.model.iam.*;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>映射默认策略版本的持久化字段，不写入数据库生成列。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@TableName(value = "iam_default_policy_revision", autoResultMap = true)
public class IamDefaultPolicyRevisionEntity {
    /** 记录 ID。 */
    @TableId(value = "id", type = IdType.INPUT)
    private BigInteger id;

    /** 角色种类。 */
    @TableField("kind")
    private DefaultPolicyKind kind;

    /** 发布版本号。 */
    @TableField("revision")
    private BigInteger revision;

    /** 策略定义 JSON 对象。 */
    @TableField("definition")
    private String definition;

}
