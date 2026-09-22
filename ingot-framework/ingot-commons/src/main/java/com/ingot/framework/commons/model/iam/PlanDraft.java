package com.ingot.framework.commons.model.iam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * <p>声明套餐包含的应用清单，修改套餐不自动改变既有租户开通。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param name 套餐名称
 * @param description 说明，可空
 * @param applicationIds 显式关联的应用 ID
 * @param status 套餐状态，可空；创建缺省启用，更新缺省保持当前状态
 */
@Schema(description = "声明套餐包含的应用清单，修改套餐不自动改变既有租户开通")
public record PlanDraft(
        @NotBlank @Size(max = 128) @Schema(description = "套餐名称", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @Schema(description = "说明，可空")
        String description,
        @NotNull @Schema(description = "显式关联的应用 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotBlank String> applicationIds,
        @Schema(description = "套餐状态，可空；创建缺省启用，更新缺省保持当前状态")
        ConfigurationStatus status) {

    /**
     * 复制应用清单。
     */
    public PlanDraft {
        if (applicationIds != null) {
            applicationIds = Collections.unmodifiableList(new ArrayList<>(applicationIds));
        }
    }
}
