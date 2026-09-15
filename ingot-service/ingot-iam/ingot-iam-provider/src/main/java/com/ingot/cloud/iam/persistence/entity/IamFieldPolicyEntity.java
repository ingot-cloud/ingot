package com.ingot.cloud.iam.persistence.entity;

import java.math.BigInteger;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ingot.framework.commons.model.iam.DefaultPolicyKind;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>保存租户字段策略引用，不复制默认策略条目。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@TableName(value = "iam_field_policy", autoResultMap = true)
public class IamFieldPolicyEntity {
    /** 所属租户 ID。 */
    @TableId(value = "tenant_id", type = IdType.INPUT)
    private BigInteger tenantId;

    /** 默认字段策略版本 ID。 */
    @TableField("default_revision_id")
    private BigInteger defaultRevisionId;

    /** 固定为 FIELD，用于引用完整性。 */
    @TableField("default_kind")
    private DefaultPolicyKind defaultKind;

    /** 递增配置版本。 */
    @TableField("version")
    private BigInteger version;
}
