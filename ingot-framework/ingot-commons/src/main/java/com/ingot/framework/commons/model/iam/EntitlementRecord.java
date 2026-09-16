package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>返回应用显式开通事实，期限和状态不代表拥有业务操作权。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param id 开通记录 ID
 * @param applicationId 应用 ID
 * @param status 显式开通或停用状态
 * @param source 开通来源
 * @param sourceId 来源记录 ID，可空
 * @param validFrom UTC 开始时间，可空
 * @param validUntil UTC 结束时间，不包含，可空
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "返回应用显式开通事实，期限和状态不代表拥有业务操作权")
public record EntitlementRecord(
        @NotBlank @Schema(description = "开通记录 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String id,
        @NotBlank @Schema(description = "应用 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String applicationId,
        @NotNull @Schema(description = "显式开通或停用状态", requiredMode = Schema.RequiredMode.REQUIRED)
        ConfigurationStatus status,
        @NotNull @Schema(description = "开通来源", requiredMode = Schema.RequiredMode.REQUIRED)
        EntitlementSource source,
         @Schema(description = "来源记录 ID，可空")
        String sourceId,
         @JsonFormat(shape = JsonFormat.Shape.STRING) @Schema(description = "UTC 开始时间，可空")
        Instant validFrom,
         @JsonFormat(shape = JsonFormat.Shape.STRING) @Schema(description = "UTC 结束时间，不包含，可空")
        Instant validUntil) {
}
