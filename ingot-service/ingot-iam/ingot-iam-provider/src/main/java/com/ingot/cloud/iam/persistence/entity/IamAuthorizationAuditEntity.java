package com.ingot.cloud.iam.persistence.entity;

import java.math.BigInteger;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.AuditChangeType;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>保存与业务同事务的脱敏审计事实，不承载凭证或通讯录原值。</p>
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@TableName(value = "iam_authorization_audit", autoResultMap = true)
public class IamAuthorizationAuditEntity {
    /** 审计记录 ID。 */
    @TableId(value = "id", type = IdType.INPUT)
    private BigInteger id;

    /** 可靠投递事件 ID。 */
    @TableField("event_id")
    private String eventId;

    /** 操作者全局账号 ID。 */
    @TableField("actor_account_id")
    private BigInteger actorAccountId;

    /** 操作者当前域成员 ID。 */
    @TableField("actor_member_id")
    private BigInteger actorMemberId;

    /** 操作者当前管理域。 */
    @TableField("domain")
    private AuthorizationDomain domain;

    /** 租户域 ID；平台域为空。 */
    @TableField("tenant_id")
    private BigInteger tenantId;

    /** 目标资源编码。 */
    @TableField("target_type")
    private String targetType;

    /** 目标业务 ID。 */
    @TableField("target_id")
    private String targetId;

    /** 变更类型。 */
    @TableField("change_type")
    private AuditChangeType changeType;

    /** 变更前脱敏 JSON。 */
    @TableField("safe_before")
    private String safeBefore;

    /** 变更后脱敏 JSON。 */
    @TableField("safe_after")
    private String safeAfter;

    /** 参与变更的版本 JSON。 */
    @TableField("revisions")
    private String revisions;

    /** 来源委派 ID，可空。 */
    @TableField("delegation_id")
    private BigInteger delegationId;

    /** 相关分配 ID，可空。 */
    @TableField("assignment_id")
    private BigInteger assignmentId;

    /** 追踪 ID，可空。 */
    @TableField("trace_id")
    private String traceId;

    /** 发生时间，UTC。 */
    @TableField("occurred_at")
    private LocalDateTime occurredAt;

    /** 成功投递时间，UTC；未投递为空。 */
    @TableField("delivered_at")
    private LocalDateTime deliveredAt;

}
