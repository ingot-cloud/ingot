package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>声明单条显式开通及期限，来源由服务器写入。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param applicationId 应用 ID
 * @param status 显式开通或停用
 * @param validFrom UTC 开始时间，可空
 * @param validUntil UTC 结束时间，不包含，可空
 */
@Schema(description = "声明单条显式开通及期限，来源由服务器写入")
public record EntitlementDraft(
        @NotBlank @Schema(description = "应用 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String applicationId,
        @NotNull @Schema(description = "显式开通或停用", requiredMode = Schema.RequiredMode.REQUIRED)
        ConfigurationStatus status,
        @JsonFormat(shape = JsonFormat.Shape.STRING) @Schema(description = "UTC 开始时间，可空")
        Instant validFrom,
        @JsonFormat(shape = JsonFormat.Shape.STRING) @Schema(description = "UTC 结束时间，不包含，可空")
        Instant validUntil) {

    /**
     * 起止时间必须形成左闭右开区间。
     *
     * @return 是否满足结构约束
     */
    @JsonIgnore
    @AssertTrue(message = "起止时间必须形成左闭右开区间")
    @Schema(hidden = true)
    public boolean isValidPeriod() {
        return validFrom == null || validUntil == null || validFrom.isBefore(validUntil);
    }
}
