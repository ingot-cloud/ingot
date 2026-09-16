package com.ingot.framework.commons.model.iam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>原子升级选定授权引用的共享基础版本，未解决冲突时拒绝提交。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param expectedVersion 升级预览使用的配置版本
 * @param newBaseRevisionId 目标共享基础版本
 * @param resolutions 全部冲突键的显式处置
 * @param assignmentIds 本次要改写的授权 ID；默认不选择既有授权
 */
@Schema(description = "原子升级选定授权引用的共享基础版本，未解决冲突时拒绝提交")
public record UpgradeInput(
        @NotBlank @Schema(description = "升级预览使用的配置版本", requiredMode = Schema.RequiredMode.REQUIRED)
        String expectedVersion,
        @NotBlank @Schema(description = "目标共享基础版本", requiredMode = Schema.RequiredMode.REQUIRED)
        String newBaseRevisionId,
        @NotNull @Schema(description = "全部冲突键的显式处置", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid UpgradeResolution> resolutions,
        @NotNull @Schema(description = "本次要改写的授权 ID；默认不选择既有授权", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotBlank String> assignmentIds) {

    /**
     * 复制提交集合，避免外部修改改变待执行命令。
     */
    public UpgradeInput {
        if (resolutions != null) {
            resolutions = Collections.unmodifiableList(new ArrayList<>(resolutions));
        }
        if (assignmentIds != null) {
            assignmentIds = Collections.unmodifiableList(new ArrayList<>(assignmentIds));
        }
    }

    /**
     * 同一冲突键不能给出多项处置，授权 ID 不得重复。
     *
     * @return 是否满足结构约束
     */
    @JsonIgnore
    @AssertTrue(message = "冲突处置键与授权 ID 不得重复")
    @Schema(hidden = true)
    public boolean isUnique() {
        if (resolutions == null || assignmentIds == null
                || resolutions.contains(null) || assignmentIds.contains(null)) {
            return true;
        }
        return resolutions.stream().map(UpgradeResolution::key).distinct().count() == resolutions.size()
                && assignmentIds.stream().distinct().count() == assignmentIds.size();
    }
}
