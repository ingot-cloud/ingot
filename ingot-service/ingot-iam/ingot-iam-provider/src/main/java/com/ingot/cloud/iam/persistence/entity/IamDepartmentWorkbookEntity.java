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
 * <p>保存租户部门表格导入任务、计数与对象存储路径。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@TableName(value = "iam_department_workbook", autoResultMap = true)
public class IamDepartmentWorkbookEntity {
    /** 任务 ID。 */
    @TableId(value = "id", type = IdType.INPUT)
    private BigInteger id;

    /** 所属租户 ID。 */
    @TableField("tenant_id")
    private BigInteger tenantId;

    /** 发起任务的租户成员 ID。 */
    @TableField("actor_member_id")
    private BigInteger actorMemberId;

    /** 导入任务种类。 */
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

    /** 导入预留范围 JSON 数组。 */
    @TableField("department_ids")
    private String departmentIds;

    /** 成功失败计数与逐行原因 JSON。 */
    @TableField("result")
    private String result;

    /** 原始工作簿对象路径 {@code bucket/objectName}。 */
    @TableField("workbook_path")
    private String workbookPath;

    /** 错误报告对象路径 {@code bucket/objectName}。 */
    @TableField("error_report_path")
    private String errorReportPath;

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
