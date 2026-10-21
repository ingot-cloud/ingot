package com.ingot.framework.commons.model.download;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * <p>「我的下载」列表中的一条导出任务，不暴露对象存储路径。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param id 任务 ID
 * @param source 来源编码，例如 {@code iam.member-workbook}
 * @param title 展示名称
 * @param status 当前状态；已到期但尚未清理的任务按 {@link DownloadTaskStatus#EXPIRED} 返回
 * @param filename 下载文件名；成功后有值
 * @param fileSize 文件字节数；成功后有值
 * @param failureReason 可展示的失败原因；仅失败时有值
 * @param createdAt 登记时间，UTC
 * @param completedAt 完成时间，UTC；未完成时为空
 * @param expiresAt 文件过期时间，UTC
 */
@Schema(description = "下载任务")
public record DownloadTask(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String source,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) DownloadTaskStatus status,
        String filename,
        Long fileSize,
        String failureReason,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant createdAt,
        Instant completedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant expiresAt) {
}
