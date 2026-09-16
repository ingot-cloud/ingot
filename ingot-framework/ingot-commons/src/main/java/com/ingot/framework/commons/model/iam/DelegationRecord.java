package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>返回可见委派定义及撤销状态。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param id 委派 ID
 * @param delegation 当前委派限制
 * @param status 生效或撤销状态
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "返回可见委派定义及撤销状态")
public record DelegationRecord(
        @NotBlank @Schema(description = "委派 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String id,
        @NotNull @Valid @Schema(description = "当前委派限制", requiredMode = Schema.RequiredMode.REQUIRED)
        DelegationInput delegation,
        @NotNull @Schema(description = "生效或撤销状态", requiredMode = Schema.RequiredMode.REQUIRED)
        GrantStatus status) {
}
