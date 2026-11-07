package com.ingot.cloud.iam.application.catalog.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * <p>创建绑定资源的精确操作；保存时拼成应用编码:资源编码:末段，不得包含通配符。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param resourceId 所属资源 ID
 * @param code 操作码末段或完整码
 * @param name 操作名称
 */
@Schema(description = "创建绑定资源的精确操作；保存时拼成应用编码:资源编码:末段，不得包含通配符")
public record ActionDraft(
        @NotBlank @Schema(description = "所属资源 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String resourceId,
        @NotBlank @Size(max = 192) @Pattern(regexp = "[^*]+")
        @Schema(description = "操作码末段或完整码", requiredMode = Schema.RequiredMode.REQUIRED)
        String code,
        @NotBlank @Size(max = 128) @Schema(description = "操作名称", requiredMode = Schema.RequiredMode.REQUIRED)
        String name) {
}
