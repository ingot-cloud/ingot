package com.ingot.cloud.iam.persistence.entity;

import java.math.BigInteger;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ingot.framework.commons.model.iam.ExportTaskStatus;
import com.ingot.framework.commons.model.iam.MemberWorkbookKind;
import com.ingot.framework.commons.model.iam.MemberWorkbookMode;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>保存租户成员表格导入导出任务、计数与文件产物，不落密码到结果 JSON。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@TableName(value = "iam_member_workbook", autoResultMap = true)
public class IamMemberWorkbookEntity {
    /** 任务 ID。 */
    @TableId(value = "id", type = IdType.INPUT)
    private BigInteger id;

    /** 所属租户 ID。 */
    @TableField("tenant_id")
    private BigInteger tenantId;

    /** 发起任务的租户成员 ID。 */
    @TableField("actor_member_id")
    private BigInteger actorMemberId;

    /** 导出或导入。 */
    @TableField("kind")
    private MemberWorkbookKind kind;

    /** 创建或更新。 */
    @TableField("mode")
    private MemberWorkbookMode mode;

    /** 任务生命周期状态。 */
    @TableField("status")
    private ExportTaskStatus status;

    /** 登记时的组织版本。 */
    @TableField("tenant_version")
    private String tenantVersion;

    /** 导出范围成员 ID JSON 数组。 */
    @TableField("member_ids")
    private String memberIds;

    /** 成功失败计数与逐行原因 JSON。 */
    @TableField("result")
    private String result;

    /** 导出或上传后的工作簿。 */
    @TableField("workbook")
    private byte[] workbook;

    /** 失败报告。 */
    @TableField("error_report")
    private byte[] errorReport;

    /** 一次性初始密码文件。 */
    @TableField("password_file")
    private byte[] passwordFile;

    /** 初始密码是否已取走。 */
    @TableField("password_downloaded")
    private Boolean passwordDownloaded;

    /** 整份失败原因编码。 */
    @TableField("failure_reason")
    private String failureReason;

    /** 登记时间，UTC。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 完成时间，UTC。 */
    @TableField("completed_at")
    private LocalDateTime completedAt;

    /** 过期时间，UTC。 */
    @TableField("expires_at")
    private LocalDateTime expiresAt;
}
