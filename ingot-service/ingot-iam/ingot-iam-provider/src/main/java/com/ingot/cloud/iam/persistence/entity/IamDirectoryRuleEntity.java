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
 * <p>保存通讯录允许或禁止目标规则。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@TableName(value = "iam_directory_rule", autoResultMap = true)
public class IamDirectoryRuleEntity {
    /** 规则 ID。 */
    @TableId(value = "id", type = IdType.INPUT)
    private BigInteger id;

    /** 所属租户 ID。 */
    @TableField("tenant_id")
    private BigInteger tenantId;

    /** 允许或禁止。 */
    @TableField("effect")
    private PolicyEffect effect;

    /** 查看者选择器 ID。 */
    @TableField("viewer_selector_id")
    private BigInteger viewerSelectorId;

    /** 目标选择器 ID。 */
    @TableField("target_selector_id")
    private BigInteger targetSelectorId;

}
