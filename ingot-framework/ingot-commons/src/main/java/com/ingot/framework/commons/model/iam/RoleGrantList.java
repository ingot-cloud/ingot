package com.ingot.framework.commons.model.iam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * <p>一次返回固定版本的完整绑定权限，当前授权入口附带同一版本的轻量摘要。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param items 当前绑定权限
 * @param revision 当前授权对应的发布版本；无版本或固定版本入口时可省略
 */
@Schema(description = "角色当前绑定权限列表，不分页")
public record RoleGrantList(
        @NotNull @Schema(description = "当前绑定权限", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid RoleGrantRecord> items,
        @Valid @JsonInclude(JsonInclude.Include.NON_NULL)
        @Schema(description = "当前授权对应的固定版本摘要；无已发布版本时省略") RoleRevisionSummary revision) {

    /** 固定版本入口或无发布版本时保留仅返回权限列表的调用方式。 */
    public RoleGrantList(List<RoleGrantRecord> items) {
        this(items, null);
    }

    /**
     * 复制列表，避免外部修改已经计算的详情结果。
     */
    public RoleGrantList {
        if (items != null) {
            items = Collections.unmodifiableList(new ArrayList<>(items));
        }
    }
}
