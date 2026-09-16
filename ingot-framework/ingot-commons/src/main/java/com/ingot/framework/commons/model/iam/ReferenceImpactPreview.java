package com.ingot.framework.commons.model.iam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>返回组或委派变更中允许披露的引用影响，隐藏对象不以零计数代替。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param affectedAssignmentIds 允许披露的授权 ID
 * @param impactSummary 允许披露的影响摘要
 */
@Schema(description = "返回组或委派变更中允许披露的引用影响，隐藏对象不以零计数代替")
public record ReferenceImpactPreview(
        @NotNull @Schema(description = "允许披露的授权 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotBlank String> affectedAssignmentIds,
        @NotNull @Valid @Schema(description = "允许披露的影响摘要", requiredMode = Schema.RequiredMode.REQUIRED)
        ImpactSummary impactSummary) {

    /**
     * 复制授权 ID 集合。
     */
    public ReferenceImpactPreview {
        if (affectedAssignmentIds != null) {
            affectedAssignmentIds = Collections.unmodifiableList(new ArrayList<>(affectedAssignmentIds));
        }
    }
}
