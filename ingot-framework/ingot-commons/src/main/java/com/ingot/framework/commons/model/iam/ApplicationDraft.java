package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * <p>创建应用目录项，基础应用标记仅允许租户域。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param code 应用命名空间
 * @param domain 应用所属管理域
 * @param name 应用名称
 * @param description 说明，可空
 * @param icon 图标，可空
 * @param sortOrder 展示顺序
 * @param baseline 是否在组织初始化时开通；仅租户域可为 true
 */
@Schema(description = "创建应用目录项，基础应用标记仅允许租户域")
public record ApplicationDraft(
        @NotBlank @Size(max = 64) @Schema(description = "应用命名空间", requiredMode = Schema.RequiredMode.REQUIRED)
        String code,
        @NotNull @Schema(description = "应用所属管理域", requiredMode = Schema.RequiredMode.REQUIRED)
        AuthorizationDomain domain,
        @NotBlank @Size(max = 128) @Schema(description = "应用名称", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @Schema(description = "说明，可空")
        String description,
        @Schema(description = "图标，可空")
        String icon,
        @Schema(description = "展示顺序", requiredMode = Schema.RequiredMode.REQUIRED)
        int sortOrder,
        @Schema(description = "是否在组织初始化时开通；仅租户域可为 true", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean baseline) {

    /**
     * 平台应用不能作为组织初始化基础开通。
     *
     * @return 域与基础标记是否匹配
     */
    @JsonIgnore
    @AssertTrue(message = "只有租户域应用可以标记为基础开通")
    @Schema(hidden = true)
    public boolean isBaselineDomainValid() {
        return domain != AuthorizationDomain.PLATFORM || !baseline;
    }
}
