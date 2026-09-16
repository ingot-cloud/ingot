package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>返回允许披露的授权引用统计，受限数量不伪装成零。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param assignments 可披露的授权数量；不可披露时省略
 * @param delegations 可披露的委派数量；不可披露时省略
 * @param restricted 是否有未披露的引用信息
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "返回允许披露的授权引用统计，受限数量不伪装成零")
public record UsageSummary(
         @PositiveOrZero @JsonSerialize(using = IamCountSerializer.class) @Schema(description = "可披露的授权数量；不可披露时省略")
        Long assignments,
         @PositiveOrZero @JsonSerialize(using = IamCountSerializer.class) @Schema(description = "可披露的委派数量；不可披露时省略")
        Long delegations,
         @Schema(description = "是否有未披露的引用信息", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean restricted) {
}
