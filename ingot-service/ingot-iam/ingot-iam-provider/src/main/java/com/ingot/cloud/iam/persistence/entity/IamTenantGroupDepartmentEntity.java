package com.ingot.cloud.iam.persistence.entity;

import java.math.BigInteger;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>保存租户组的部门成员来源。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@TableName(value = "iam_tenant_group_department", autoResultMap = true)
public class IamTenantGroupDepartmentEntity {
    /** 所属租户 ID。 */
    @TableField("tenant_id")
    private BigInteger tenantId;

    /** 租户组 ID。 */
    @TableField("group_id")
    private BigInteger groupId;

    /** 部门 ID。 */
    @TableField("department_id")
    private BigInteger departmentId;

    /** 是否包含下级部门。 */
    @TableField("include_descendants")
    private Boolean includeDescendants;

}
