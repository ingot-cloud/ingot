package com.ingot.framework.commons.model.iam;

import java.util.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>为指定查看者执行受限只读策略预览。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param policyDraft 完整类型化草稿
 * @param viewerMemberId 当前租户内待预览查看者的成员 ID
 * @param target 可选目标成员 ID；必须在操作者可查看范围内
 */
@Schema(description = "为指定查看者执行受限只读策略预览")
public record PolicyPreviewInput(
        @NotNull @Valid @Schema(description = "完整类型化草稿", requiredMode = Schema.RequiredMode.REQUIRED)
        PolicyDraft policyDraft,
        @NotBlank @Schema(description = "当前租户内待预览查看者的成员 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String viewerMemberId,
        @Valid @Schema(description = "可选目标成员 ID；必须在操作者可查看范围内")
        String target) {
}
