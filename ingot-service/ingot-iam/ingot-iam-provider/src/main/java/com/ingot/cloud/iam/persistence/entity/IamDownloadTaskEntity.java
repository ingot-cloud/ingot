package com.ingot.cloud.iam.persistence.entity;

import java.math.BigInteger;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ingot.framework.commons.model.download.DownloadTaskStatus;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>下载中心任务行，文件只记录对象存储路径。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@Setter
@TableName("iam_download_task")
public class IamDownloadTaskEntity {
    /** 任务 ID。 */
    @TableId(value = "id", type = IdType.INPUT)
    private BigInteger id;

    /** 发起人认证域。 */
    @TableField("domain")
    private AuthorizationDomain domain;

    /** 所属租户 ID；平台域为空。 */
    @TableField("tenant_id")
    private BigInteger tenantId;

    /** 发起导出的成员 ID。 */
    @TableField("actor_member_id")
    private BigInteger actorMemberId;

    /** 导出来源编码。 */
    @TableField("source")
    private String source;

    /** 展示名称。 */
    @TableField("title")
    private String title;

    /** 任务状态。 */
    @TableField("status")
    private DownloadTaskStatus status;

    /** 下载文件名。 */
    @TableField("filename")
    private String filename;

    /** MIME 类型。 */
    @TableField("content_type")
    private String contentType;

    /** 文件字节数。 */
    @TableField("file_size")
    private Long fileSize;

    /** 对象存储路径 {@code bucket/objectName}。 */
    @TableField("object_path")
    private String objectPath;

    /** 可展示失败原因。 */
    @TableField("failure_reason")
    private String failureReason;

    /** 登记时间，UTC。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 开始生成时间，UTC。 */
    @TableField("started_at")
    private LocalDateTime startedAt;

    /** 完成时间，UTC。 */
    @TableField("completed_at")
    private LocalDateTime completedAt;

    /** 文件过期时间，UTC。 */
    @TableField("expires_at")
    private LocalDateTime expiresAt;
}
