package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>标识审计目标，不附带目标业务资料原值。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param resource 资源编码
 * @param id 目标 ID
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "标识审计目标，不附带目标业务资料原值")
public record AuditTarget(
        @NotBlank @Schema(description = "资源编码", requiredMode = Schema.RequiredMode.REQUIRED)
        String resource,
        @NotBlank @Schema(description = "目标 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String id) {
}
