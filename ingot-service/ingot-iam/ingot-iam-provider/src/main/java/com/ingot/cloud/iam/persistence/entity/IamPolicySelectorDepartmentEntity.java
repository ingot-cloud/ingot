package com.ingot.cloud.iam.persistence.entity;

import java.math.BigInteger;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>保存策略选择器中的部门引用，是否含下级由规则自身声明。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@TableName(value = "iam_policy_selector_department", autoResultMap = true)
public class IamPolicySelectorDepartmentEntity {
    /** 所属租户 ID。 */
    @TableField("tenant_id")
    private BigInteger tenantId;

    /** 选择器 ID。 */
    @TableField("selector_id")
    private BigInteger selectorId;

    /** 部门 ID。 */
    @TableField("department_id")
    private BigInteger departmentId;

    /** 是否包含下级部门。 */
    @TableField("include_descendants")
    private Boolean includeDescendants;
}
