package com.ingot.framework.commons.model.iam;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>返回当前操作者最近一次导入任务摘要，只记条数。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param id 任务 ID
 * @param kind 导出或导入
 * @param mode 创建或更新
 * @param status 生命周期状态
 * @param createdAt 登记时间，UTC
 * @param expiresAt 过期时间，UTC
 * @param successCount 成功行数
 * @param failCount 失败行数
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "返回当前操作者最近一次导入任务摘要")
public record MemberWorkbookHistoryItem(
        @NotBlank @Schema(description = "任务 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String id,
        @NotNull @Schema(description = "导出或导入", requiredMode = Schema.RequiredMode.REQUIRED)
        MemberWorkbookKind kind,
        @NotNull @Schema(description = "创建或更新", requiredMode = Schema.RequiredMode.REQUIRED)
        MemberWorkbookMode mode,
        @NotNull @Schema(description = "生命周期状态", requiredMode = Schema.RequiredMode.REQUIRED)
        ExportTaskStatus status,
        @NotNull @Schema(description = "登记时间，UTC", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant createdAt,
        @NotNull @Schema(description = "过期时间，UTC", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant expiresAt,
        @Schema(description = "成功行数")
        Integer successCount,
        @Schema(description = "失败行数")
        Integer failCount) {
}
