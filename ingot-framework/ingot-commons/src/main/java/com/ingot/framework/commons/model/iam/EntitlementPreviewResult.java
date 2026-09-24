package com.ingot.framework.commons.model.iam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * <p>返回开通或套餐应用的可见影响，不把开通等同于业务授权。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param entitlements 服务器解析后的开通并集
 * @param impactSummary 允许披露的影响摘要
 */
@Schema(description = "返回开通或套餐应用的可见影响，不把开通等同于业务授权")
public record EntitlementPreviewResult(
        @NotNull @Schema(description = "服务器解析后的开通并集", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid EntitlementPreviewItem> entitlements,
        @NotNull @Valid @Schema(description = "允许披露的影响摘要", requiredMode = Schema.RequiredMode.REQUIRED)
        ImpactSummary impactSummary) {

    /**
     * 复制开通清单。
     */
    public EntitlementPreviewResult {
        if (entitlements != null) {
            entitlements = Collections.unmodifiableList(new ArrayList<>(entitlements));
        }
    }
}
