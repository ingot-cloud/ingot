package com.ingot.framework.commons.model.iam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>授权选择目录中的启用资源，内嵌该资源全部启用操作与范围能力。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param id 资源 ID
 * @param code 应用内资源编码
 * @param name 资源名称
 * @param scopeCapabilities 资源允许的范围种类
 * @param actions 该资源下的启用操作
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "授权选择目录中的启用资源，内嵌该资源全部启用操作")
public record GrantCatalogResource(
        @NotBlank @Schema(description = "资源 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String id,
        @NotBlank @Schema(description = "应用内资源编码", requiredMode = Schema.RequiredMode.REQUIRED)
        String code,
        @NotBlank @Schema(description = "资源名称", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @NotNull @Schema(description = "资源允许的范围种类", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid ScopeKind> scopeCapabilities,
        @NotNull @Schema(description = "该资源下的启用操作", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid GrantCatalogAction> actions) {

    /**
     * 复制输出集合，避免外部修改改变已经计算的目录视图。
     */
    public GrantCatalogResource {
        if (scopeCapabilities != null) {
            scopeCapabilities = Collections.unmodifiableList(new ArrayList<>(scopeCapabilities));
        }
        if (actions != null) {
            actions = Collections.unmodifiableList(new ArrayList<>(actions));
        }
    }
}
