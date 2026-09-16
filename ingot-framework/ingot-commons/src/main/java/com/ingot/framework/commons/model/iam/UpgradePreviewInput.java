package com.ingot.framework.commons.model.iam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>预览将共享基础升级到指定版本后的三方差异，不写入任何引用。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param newBaseRevisionId 目标共享基础版本
 * @param resolutions 可选的已选冲突处置；预览可不提交
 */
@Schema(description = "预览将共享基础升级到指定版本后的三方差异，不写入任何引用")
public record UpgradePreviewInput(
        @NotBlank @Schema(description = "目标共享基础版本", requiredMode = Schema.RequiredMode.REQUIRED)
        String newBaseRevisionId,
        @Schema(description = "可选的已选冲突处置；预览可不提交")
        List<@NotNull @Valid UpgradeResolution> resolutions) {

    /**
     * 规范化可选处置集合。
     */
    public UpgradePreviewInput {
        resolutions = resolutions == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(resolutions));
    }
}
