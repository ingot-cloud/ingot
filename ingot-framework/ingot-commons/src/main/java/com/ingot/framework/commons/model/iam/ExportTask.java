package com.ingot.framework.commons.model.iam;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>返回成员导出任务在共享存储中的状态，供多实例轮询，不含成员快照。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param id 任务 ID
 * @param status 生命周期状态
 * @param version 登记时的组织版本
 * @param expiresAt 过期时间，UTC
 * @param failureCode 失败粗码；仅 {@code FAILED} 出现
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "返回成员导出任务在共享存储中的状态，供多实例轮询，不含成员快照")
public record ExportTask(
        @NotBlank @Schema(description = "任务 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String id,
        @NotNull @Schema(description = "生命周期状态", requiredMode = Schema.RequiredMode.REQUIRED)
        ExportTaskStatus status,
        @NotBlank @Schema(description = "登记时的组织版本", requiredMode = Schema.RequiredMode.REQUIRED)
        String version,
        @NotNull @Schema(description = "过期时间，UTC", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant expiresAt,
        @Schema(description = "失败粗码；仅 FAILED 出现")
        String failureCode) {
}
