package com.ingot.framework.commons.model.iam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>用角色 ID 列表替换成员的简单直接角色，空列表表示清除。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param roleIds 目标角色定义 ID，可空集合
 */
@Schema(description = "用角色 ID 列表替换成员的简单直接角色，空列表表示清除")
public record MemberRoleReplaceInput(
        @NotNull @Schema(description = "目标角色定义 ID，可空集合", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotBlank String> roleIds) {

    /**
     * 规范化角色 ID 集合。
     */
    public MemberRoleReplaceInput {
        if (roleIds != null) {
            roleIds = Collections.unmodifiableList(new ArrayList<>(roleIds));
        }
    }
}
