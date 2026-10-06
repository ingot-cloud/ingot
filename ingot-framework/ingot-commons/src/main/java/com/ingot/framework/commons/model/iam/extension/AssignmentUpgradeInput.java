package com.ingot.framework.commons.model.iam.extension;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.ingot.framework.commons.model.iam.RoleRevisionRef;

/**
 * <p>同角色分配的原子升级，目标固定到已发布版本。
 * </p>
 *
 * @param targetRevisionRef 目标固定版本
 * @param items 既有分配，最多一百条
 * @author jy
 * @since 1.0.0
 */
public record AssignmentUpgradeInput(@NotNull @Valid RoleRevisionRef targetRevisionRef,
        @NotEmpty @Size(max = AssignmentUpgradeInput.MAX_ITEMS) List<@Valid AssignmentUpgradeItem> items) {
    /** 原子升级命令的最大分配数。 */
    public static final int MAX_ITEMS = 100;

    /**
     * 固定提交集合。
     */
    public AssignmentUpgradeInput {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
