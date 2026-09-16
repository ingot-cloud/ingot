package com.ingot.framework.commons.model.iam;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * <p>更新操作名称，不能改写操作码或所属资源。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param expectedVersion 操作读取版本
 * @param name 操作名称
 */
@Schema(description = "更新操作名称，不能改写操作码或所属资源")
public record ActionUpdateInput(
        @NotBlank @Schema(description = "操作读取版本", requiredMode = Schema.RequiredMode.REQUIRED)
        String expectedVersion,
        @NotBlank @Size(max = 128) @Schema(description = "操作名称", requiredMode = Schema.RequiredMode.REQUIRED)
        String name) {
}
