package com.ingot.framework.commons.model.iam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>授权审计游标页，不返回总数；引用字典按查看人权限解析本页涉及的对象。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param items 本页审计
 * @param nextCursor 下一页游标；仅在还有更多时返回
 * @param hasMore 是否还有下一页
 * @param references 以 {@code type:id} 为键的对象名称
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "授权审计游标页")
public record AuditCursorPage(
        @NotNull @Schema(description = "本页审计", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid ResourceDetail<AuditEntry>> items,
        @Schema(description = "下一页游标；仅在还有更多时返回")
        String nextCursor,
        @Schema(description = "是否还有下一页", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean hasMore,
        @NotNull @Schema(description = "以 type:id 为键的对象名称", requiredMode = Schema.RequiredMode.REQUIRED)
        Map<@NotBlank String, @NotNull @Valid AuditReference> references) {

    /**
     * 复制集合，避免外部修改响应视图。
     */
    public AuditCursorPage {
        items = items == null ? null : Collections.unmodifiableList(new ArrayList<>(items));
        references = references == null ? null : Collections.unmodifiableMap(new LinkedHashMap<>(references));
    }
}
