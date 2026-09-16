package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>描述绑定资源的精确操作，不从导航或名称推导授权。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param id 操作 ID
 * @param applicationId 所属应用 ID
 * @param resourceId 所属资源 ID
 * @param code 全局唯一精确操作码
 * @param name 操作名称
 * @param status 操作状态
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "描述绑定资源的精确操作，不从导航或名称推导授权")
public record ActionRecord(
        @NotBlank @Schema(description = "操作 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String id,
        @NotBlank @Schema(description = "所属应用 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String applicationId,
        @NotBlank @Schema(description = "所属资源 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String resourceId,
        @NotBlank @Schema(description = "全局唯一精确操作码", requiredMode = Schema.RequiredMode.REQUIRED)
        String code,
        @NotBlank @Schema(description = "操作名称", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @NotNull @Schema(description = "操作状态", requiredMode = Schema.RequiredMode.REQUIRED)
        ConfigurationStatus status) {
}
