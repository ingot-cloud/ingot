package com.ingot.framework.commons.model.iam;

import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * <p>成员表单的角色差量，不覆盖未加载或只读分配。</p>
 * @author jy
 * @since 1.0.0
 * @param additions 新增固定版本分配
 * @param updates 既有分配范围更新
 * @param removals 撤销既有直接分配
 */
@Schema(description = "成员表单的角色差量，不覆盖未加载或只读分配")
public record MemberRoleChanges(
        @Schema(description = "新增固定版本分配") @NotNull List<@NotNull @Valid MemberRoleAssignmentDraft> additions,
        @Schema(description = "既有分配范围更新") @NotNull List<@NotNull @Valid MemberRoleScopeChange> updates,
        @Schema(description = "撤销既有直接分配") @NotNull List<@NotNull @Valid MemberRoleRemoval> removals) {
    /** 固定差量集合，防止校验后被修改。 */
    public MemberRoleChanges {
        additions = additions == null ? null : List.copyOf(additions);
        updates = updates == null ? null : List.copyOf(updates);
        removals = removals == null ? null : List.copyOf(removals);
    }
}
