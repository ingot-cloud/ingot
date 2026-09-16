package com.ingot.framework.commons.model.iam;

import java.util.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>返回应用资源支持的范围和字段能力，不暴露 SQL 表名或适配器实现。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param id 资源 ID
 * @param applicationId 所属应用 ID
 * @param code 应用内资源编码
 * @param name 资源名称
 * @param scopeCapabilities 允许的范围种类
 * @param fieldCapabilities 已注册字段能力
 * @param status 资源状态
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "返回应用资源支持的范围和字段能力，不暴露 SQL 表名或适配器实现")
public record ResourceRecord(
        @NotBlank @Schema(description = "资源 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String id,
        @NotBlank @Schema(description = "所属应用 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String applicationId,
        @NotBlank @Schema(description = "应用内资源编码", requiredMode = Schema.RequiredMode.REQUIRED)
        String code,
        @NotBlank @Schema(description = "资源名称", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @NotNull @Schema(description = "允许的范围种类", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull ScopeKind> scopeCapabilities,
        @NotNull @Schema(description = "已注册字段能力", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid FieldCapability> fieldCapabilities,
        @NotNull @Schema(description = "资源状态", requiredMode = Schema.RequiredMode.REQUIRED)
        ConfigurationStatus status) {

    /**
     * 复制输出集合，避免外部修改改变已经计算的响应视图；必填空引用由校验拒绝。
     */
    public ResourceRecord {
        if (scopeCapabilities != null) {
            scopeCapabilities = Collections.unmodifiableList(new ArrayList<>(scopeCapabilities));
        }
        if (fieldCapabilities != null) {
            fieldCapabilities = Collections.unmodifiableList(new ArrayList<>(fieldCapabilities));
        }
    }
}
