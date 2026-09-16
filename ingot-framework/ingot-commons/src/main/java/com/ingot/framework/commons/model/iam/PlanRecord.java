package com.ingot.framework.commons.model.iam;

import java.util.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>返回套餐应用清单，修改套餐不自动改变既有租户开通。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param id 套餐 ID
 * @param name 套餐名称
 * @param description 说明，可空
 * @param applicationIds 显式关联的应用 ID
 * @param status 套餐状态
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "返回套餐应用清单，修改套餐不自动改变既有租户开通")
public record PlanRecord(
        @NotBlank @Schema(description = "套餐 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String id,
        @NotBlank @Schema(description = "套餐名称", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
         @Schema(description = "说明，可空")
        String description,
        @NotNull @Schema(description = "显式关联的应用 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotBlank String> applicationIds,
        @NotNull @Schema(description = "套餐状态", requiredMode = Schema.RequiredMode.REQUIRED)
        ConfigurationStatus status) {

    /**
     * 复制输出集合，避免外部修改改变已经计算的响应视图；必填空引用由校验拒绝。
     */
    public PlanRecord {
        if (applicationIds != null) {
            applicationIds = Collections.unmodifiableList(new ArrayList<>(applicationIds));
        }
    }
}
