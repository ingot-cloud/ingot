package com.ingot.framework.commons.model.iam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * <p>角色最新已发布版本的当前绑定权限列表，不分页一次返回。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param items 当前绑定权限
 */
@Schema(description = "角色当前绑定权限列表，不分页")
public record RoleGrantList(
        @NotNull @Schema(description = "当前绑定权限", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid RoleGrantRecord> items) {

    /**
     * 复制列表，避免外部修改已经计算的详情结果。
     */
    public RoleGrantList {
        if (items != null) {
            items = Collections.unmodifiableList(new ArrayList<>(items));
        }
    }
}
