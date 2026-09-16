package com.ingot.framework.commons.model.iam;

import java.util.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>返回三方比较结果和可见受影响授权，不隐式升级任何引用。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param version 预览使用的配置版本
 * @param oldBaseRevisionId 旧基础版本 ID
 * @param newBaseRevisionId 新基础版本 ID
 * @param changes 基础版本变化
 * @param conflicts 未解决冲突
 * @param effectiveResult 仅在定义可合成时提供，可空
 * @param affectedAssignments 允许披露的受影响授权 ID
 * @param impactSummary 允许披露的影响摘要
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "返回三方比较结果和可见受影响授权，不隐式升级任何引用")
public record UpgradePreview(
        @NotBlank @Schema(description = "预览使用的配置版本", requiredMode = Schema.RequiredMode.REQUIRED)
        String version,
        @NotBlank @Schema(description = "旧基础版本 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String oldBaseRevisionId,
        @NotBlank @Schema(description = "新基础版本 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String newBaseRevisionId,
        @NotNull @Schema(description = "基础版本变化", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid UpgradeChange> changes,
        @NotNull @Schema(description = "未解决冲突", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid UpgradeConflict> conflicts,
        @Valid @Schema(description = "仅在定义可合成时提供，可空")
        EffectiveRole effectiveResult,
        @NotNull @Schema(description = "允许披露的受影响授权 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotBlank String> affectedAssignments,
        @NotNull @Valid @Schema(description = "允许披露的影响摘要", requiredMode = Schema.RequiredMode.REQUIRED)
        ImpactSummary impactSummary) {

    /**
     * 复制输出集合，避免外部修改改变已经计算的响应视图；必填空引用由校验拒绝。
     */
    public UpgradePreview {
        if (changes != null) {
            changes = Collections.unmodifiableList(new ArrayList<>(changes));
        }
        if (conflicts != null) {
            conflicts = Collections.unmodifiableList(new ArrayList<>(conflicts));
        }
        if (affectedAssignments != null) {
            affectedAssignments = Collections.unmodifiableList(new ArrayList<>(affectedAssignments));
        }
    }
}
