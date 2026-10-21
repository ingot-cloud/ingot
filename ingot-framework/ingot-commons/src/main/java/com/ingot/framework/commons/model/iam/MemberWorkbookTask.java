package com.ingot.framework.commons.model.iam;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>返回成员表格任务状态与计数，供轮询，不含文件与密码明文。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param id 任务 ID
 * @param kind 导出或导入
 * @param mode 创建或更新
 * @param status 生命周期状态
 * @param version 登记时的组织版本
 * @param createdAt 登记时间，UTC
 * @param expiresAt 过期时间，UTC
 * @param failureCode 整份失败粗码；仅 FAILED 出现
 * @param successCount 已成功行数；进行中可空
 * @param failCount 失败行数；进行中可空
 * @param passwordAvailable 是否仍可下载一次性密码
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "返回成员表格任务状态与计数，供轮询")
public record MemberWorkbookTask(
        @NotBlank @Schema(description = "任务 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String id,
        @NotNull @Schema(description = "导出或导入", requiredMode = Schema.RequiredMode.REQUIRED)
        MemberWorkbookKind kind,
        @NotNull @Schema(description = "创建或更新", requiredMode = Schema.RequiredMode.REQUIRED)
        MemberWorkbookMode mode,
        @NotNull @Schema(description = "生命周期状态", requiredMode = Schema.RequiredMode.REQUIRED)
        ExportTaskStatus status,
        @NotBlank @Schema(description = "登记时的组织版本", requiredMode = Schema.RequiredMode.REQUIRED)
        String version,
        @NotNull @Schema(description = "登记时间，UTC", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant createdAt,
        @NotNull @Schema(description = "过期时间，UTC", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant expiresAt,
        @Schema(description = "整份失败粗码；仅 FAILED 出现")
        String failureCode,
        @Schema(description = "已成功行数")
        Integer successCount,
        @Schema(description = "失败行数")
        Integer failCount,
        @Schema(description = "是否仍可下载一次性密码")
        Boolean passwordAvailable) {
}
