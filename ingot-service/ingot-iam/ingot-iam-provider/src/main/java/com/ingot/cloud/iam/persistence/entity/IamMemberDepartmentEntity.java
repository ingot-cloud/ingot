package com.ingot.cloud.iam.persistence.entity;

import java.math.BigInteger;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.ingot.framework.commons.model.iam.MemberStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>保存租户内成员任职关系，所有访问必须限定完整关系归属。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@TableName(value = "iam_member_department", autoResultMap = true)
public class IamMemberDepartmentEntity {
    /** 所属租户 ID。 */
    @TableField("tenant_id")
    private BigInteger tenantId;

    /** 租户成员 ID。 */
    @TableField("member_id")
    private BigInteger memberId;

    /** 所属部门 ID。 */
    @TableField("department_id")
    private BigInteger departmentId;

    /** 是否主部门；生成列由数据库维护。 */
    @TableField("is_primary")
    private Boolean isPrimary;

}
