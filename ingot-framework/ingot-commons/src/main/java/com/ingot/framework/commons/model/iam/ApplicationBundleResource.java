package com.ingot.framework.commons.model.iam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * <p>整包创建时的资源及其操作，用临时 ID 在同一次请求内被菜单引用。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param tempId 客户端临时 ID
 * @param code 应用内资源编码
 * @param name 资源名称
 * @param scopeCapabilities 允许的范围种类
 * @param fieldCapabilities 已注册字段能力
 * @param actions 该资源下的精确操作
 */
@Schema(description = "整包创建时的资源及其操作，用临时 ID 在同一次请求内被菜单引用")
public record ApplicationBundleResource(
        @NotBlank @Size(max = 64) @Schema(description = "客户端临时 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String tempId,
        @NotBlank @Size(max = 64) @Schema(description = "应用内资源编码", requiredMode = Schema.RequiredMode.REQUIRED)
        String code,
        @NotBlank @Size(max = 128) @Schema(description = "资源名称", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @NotNull @Schema(description = "允许的范围种类", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull ScopeKind> scopeCapabilities,
        @NotNull @Schema(description = "已注册字段能力", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid FieldCapability> fieldCapabilities,
        @NotNull @Schema(description = "该资源下的精确操作", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid ApplicationBundleAction> actions) {

    /**
     * 复制能力与操作列表。
     */
    public ApplicationBundleResource {
        if (scopeCapabilities != null) {
            scopeCapabilities = Collections.unmodifiableList(new ArrayList<>(scopeCapabilities));
        }
        if (fieldCapabilities != null) {
            fieldCapabilities = Collections.unmodifiableList(new ArrayList<>(fieldCapabilities));
        }
        if (actions != null) {
            actions = Collections.unmodifiableList(new ArrayList<>(actions));
        }
    }
}
