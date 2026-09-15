package com.ingot.cloud.iam.persistence.entity;

import java.math.BigInteger;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.ingot.framework.commons.model.iam.MemberStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>保存租户内部门树节点。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@TableName(value = "iam_department", autoResultMap = true)
public class IamDepartmentEntity {
    /** 部门 ID。 */
    @TableId(value = "id", type = IdType.INPUT)
    private BigInteger id;

    /** 所属租户 ID。 */
    @TableField("tenant_id")
    private BigInteger tenantId;

    /** 父部门 ID，根节点为空。 */
    @TableField("parent_id")
    private BigInteger parentId;

    /** 部门名称。 */
    @TableField("name")
    private String name;

    /** 同级排序值。 */
    @TableField("sort_order")
    private Integer sortOrder;

    /** 递增部门版本。 */
    @TableField("version")
    private BigInteger version;

    /** 创建时间，UTC。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 更新时间，UTC。 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

}
