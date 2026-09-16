package com.ingot.framework.commons.model.iam;

import java.util.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>声明应用的有效人群，开通不自动授予操作。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param kind ALL 或 SELECTED
 * @param selection SELECTED 时必填；ALL 不携带
 * @param groupIds 当前租户组；仅 SELECTED 可指定
 */
@Schema(description = "声明应用的有效人群，开通不自动授予操作")
public record AudienceDraft(
        @NotNull @Valid @Schema(description = "ALL 或 SELECTED", requiredMode = Schema.RequiredMode.REQUIRED)
        AudienceKind kind,
        @Valid @Schema(description = "SELECTED 时必填；ALL 不携带")
        Selection selection,
        @NotNull @Schema(description = "当前租户组；仅 SELECTED 可指定", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotBlank String> groupIds) {

    /**
     * 固定输入集合快照；必填空引用由 Bean Validation 拒绝。
     */
    public AudienceDraft {
        groupIds = groupIds == null ? null : Collections.unmodifiableList(new ArrayList<>(groupIds));
    }

    /**
     * 仅 SELECTED 人群可携带成员部门和组。
     * @return 是否符合结构约束，实体归属仍须服务端校验
     */
    @AssertTrue(message = "仅 SELECTED 人群可携带成员部门和组")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isSelectionCompatible() {
        return kind == null || (kind == AudienceKind.SELECTED ? selection != null
                : selection == null && groupIds != null && groupIds.isEmpty());
    }
}
