package com.ingot.framework.commons.model.iam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>更新资源名称与能力声明，不能改写所属应用或编码。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param expectedVersion 资源读取版本
 * @param name 资源名称
 * @param scopeCapabilities 允许的范围种类
 * @param fieldCapabilities 已注册字段能力
 */
@Schema(description = "更新资源名称与能力声明，不能改写所属应用或编码")
public record ResourceUpdateInput(
        @NotBlank @Schema(description = "资源读取版本", requiredMode = Schema.RequiredMode.REQUIRED)
        String expectedVersion,
        @NotBlank @Schema(description = "资源名称", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @NotNull @Schema(description = "允许的范围种类", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull ScopeKind> scopeCapabilities,
        @NotNull @Schema(description = "已注册字段能力", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid FieldCapability> fieldCapabilities) {

    /**
     * 复制能力集合。
     */
    public ResourceUpdateInput {
        if (scopeCapabilities != null) {
            scopeCapabilities = Collections.unmodifiableList(new ArrayList<>(scopeCapabilities));
        }
        if (fieldCapabilities != null) {
            fieldCapabilities = Collections.unmodifiableList(new ArrayList<>(fieldCapabilities));
        }
    }
}
