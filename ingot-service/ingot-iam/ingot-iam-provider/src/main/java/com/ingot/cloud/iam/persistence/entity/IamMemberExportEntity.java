package com.ingot.cloud.iam.persistence.entity;

import java.math.BigInteger;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ingot.framework.commons.model.iam.ExportTaskStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>保存租户成员导出任务状态与成员 ID 快照，不落字段原值。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@TableName(value = "iam_member_export", autoResultMap = true)
public class IamMemberExportEntity {
    /** 导出任务 ID。 */
    @TableId(value = "id", type = IdType.INPUT)
    private BigInteger id;

    /** 所属租户 ID。 */
    @TableField("tenant_id")
    private BigInteger tenantId;

    /** 发起导出的租户成员 ID。 */
    @TableField("actor_member_id")
    private BigInteger actorMemberId;

    /** 登记时的组织版本。 */
    @TableField("tenant_version")
    private String tenantVersion;

    /** 任务生命周期状态。 */
    @TableField("status")
    private ExportTaskStatus status;

    /** 快照成员 ID JSON 数组；未成功时为空。 */
    @TableField("member_ids")
    private String memberIds;

    /** 失败原因编码，不含个人资料。 */
    @TableField("failure_reason")
    private String failureReason;

    /** 登记时间，UTC。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 完成时间，UTC；进行中为空。 */
    @TableField("completed_at")
    private LocalDateTime completedAt;

    /** 过期时间，UTC；到期后快照不可下载。 */
    @TableField("expires_at")
    private LocalDateTime expiresAt;
}
