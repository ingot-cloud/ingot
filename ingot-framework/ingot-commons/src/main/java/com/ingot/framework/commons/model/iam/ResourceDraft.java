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
 * <p>声明应用内资源及其允许配置的范围和字段能力。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param code 应用内资源编码
 * @param name 资源名称
 * @param scopeCapabilities 允许的范围种类
 * @param fieldCapabilities 已注册字段能力
 */
@Schema(description = "声明应用内资源及其允许配置的范围和字段能力")
public record ResourceDraft(
        @NotBlank @Size(max = 64) @Schema(description = "应用内资源编码", requiredMode = Schema.RequiredMode.REQUIRED)
        String code,
        @NotBlank @Size(max = 128) @Schema(description = "资源名称", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @NotNull @Schema(description = "允许的范围种类", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull ScopeKind> scopeCapabilities,
        @NotNull @Schema(description = "已注册字段能力", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid FieldCapability> fieldCapabilities) {

    /**
     * 复制能力集合。
     */
    public ResourceDraft {
        if (scopeCapabilities != null) {
            scopeCapabilities = Collections.unmodifiableList(new ArrayList<>(scopeCapabilities));
        }
        if (fieldCapabilities != null) {
            fieldCapabilities = Collections.unmodifiableList(new ArrayList<>(fieldCapabilities));
        }
    }
}
