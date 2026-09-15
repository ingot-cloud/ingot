package com.ingot.cloud.iam.persistence.entity;

import java.math.BigInteger;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.ingot.framework.commons.model.iam.*;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>映射角色分配的持久化字段，不写入数据库生成列。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@TableName(value = "iam_role_assignment", autoResultMap = true)
public class IamRoleAssignmentEntity {
    /** 记录 ID。 */
    @TableId(value = "id", type = IdType.INPUT)
    private BigInteger id;

    /** 授权域。 */
    @TableField("domain")
    private AuthorizationDomain domain;

    /** 所属租户 ID。 */
    @TableField("tenant_id")
    private BigInteger tenantId;

    /** 被授权主体类型。 */
    @TableField("subject_type")
    private SubjectType subjectType;

    /** 平台成员 ID。 */
    @TableField("platform_member_id")
    private BigInteger platformMemberId;

    /** 平台组 ID。 */
    @TableField("platform_group_id")
    private BigInteger platformGroupId;

    /** 租户成员 ID。 */
    @TableField("tenant_member_id")
    private BigInteger tenantMemberId;

    /** 租户组 ID。 */
    @TableField("tenant_group_id")
    private BigInteger tenantGroupId;

    /** 角色版本 ID。 */
    @TableField("revision_id")
    private BigInteger revisionId;

    /** 角色版本种类。 */
    @TableField("revision_kind")
    private RoleKind revisionKind;

    /** 范围参数绑定 JSON 对象。 */
    @TableField("scope_bindings")
    private String scopeBindings;

    /** 委派来源 ID。 */
    @TableField("delegation_grant_id")
    private BigInteger delegationGrantId;

    /** 生效时间，使用 UTC。 */
    @TableField("valid_from")
    private LocalDateTime validFrom;

    /** 失效时间，使用 UTC。 */
    @TableField("valid_until")
    private LocalDateTime validUntil;

    /** 授权状态。 */
    @TableField("status")
    private GrantStatus status;

    /** 授权来源。 */
    @TableField("source")
    private AssignmentSource source;

    /** 配置版本。 */
    @TableField("version")
    private BigInteger version;

    /** 创建时间，使用 UTC。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

}
