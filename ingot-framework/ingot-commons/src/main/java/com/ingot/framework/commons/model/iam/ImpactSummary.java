package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>返回操作者允许知晓的变更影响，隐藏对象数量不通过统计泄露。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param affectedMembers 可披露受影响成员数；受限时省略
 * @param affectedAssignments 可披露受影响授权数；受限时省略
 * @param affectedDelegations 可披露受影响委派数；受限时省略
 * @param restricted 是否存在无法披露的影响明细
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "返回操作者允许知晓的变更影响，隐藏对象数量不通过统计泄露")
public record ImpactSummary(
         @PositiveOrZero @JsonSerialize(using = IamCountSerializer.class) @Schema(description = "可披露受影响成员数；受限时省略")
        Long affectedMembers,
         @PositiveOrZero @JsonSerialize(using = IamCountSerializer.class) @Schema(description = "可披露受影响授权数；受限时省略")
        Long affectedAssignments,
         @PositiveOrZero @JsonSerialize(using = IamCountSerializer.class) @Schema(description = "可披露受影响委派数；受限时省略")
        Long affectedDelegations,
         @Schema(description = "是否存在无法披露的影响明细", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean restricted) {
}
