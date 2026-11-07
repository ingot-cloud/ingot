package com.ingot.cloud.iam.application.catalog.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * <p>展示当前应用已绑定的一名成员，名称来自绑定查询，不回填候选列表。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param id 租户成员 ID
 * @param name 显示名称；缺失或空白时为 {@link AudienceView#RESTRICTED_NAME}
 * @param avatar 库存头像路径，可空；响应签发时效链接
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "展示当前应用已绑定的一名成员")
public record AudienceMemberView(
        @NotBlank @Schema(description = "租户成员 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String id,
        @NotBlank @Schema(description = "显示名称；缺失时为信息受限", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @Schema(description = "库存头像路径，可空")
        String avatar) {
}
