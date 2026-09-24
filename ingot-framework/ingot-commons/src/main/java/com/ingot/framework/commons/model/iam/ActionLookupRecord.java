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
 * <p>按 ID 解析出的精确操作，带应用与资源名称及范围能力。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param id 操作 ID
 * @param code 全局唯一精确操作码
 * @param name 操作名称
 * @param applicationId 所属应用 ID
 * @param applicationCode 应用编码
 * @param applicationName 应用名称
 * @param resourceId 所属资源 ID
 * @param resourceCode 资源编码
 * @param resourceName 资源名称
 * @param scopeCapabilities 资源允许的范围种类
 * @param status 操作状态
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "按 ID 解析出的精确操作，带应用与资源名称及范围能力")
public record ActionLookupRecord(
        @NotBlank @Schema(description = "操作 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String id,
        @NotBlank @Schema(description = "全局唯一精确操作码", requiredMode = Schema.RequiredMode.REQUIRED)
        String code,
        @NotBlank @Schema(description = "操作名称", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @NotBlank @Schema(description = "所属应用 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String applicationId,
        @NotBlank @Schema(description = "应用编码", requiredMode = Schema.RequiredMode.REQUIRED)
        String applicationCode,
        @NotBlank @Schema(description = "应用名称", requiredMode = Schema.RequiredMode.REQUIRED)
        String applicationName,
        @NotBlank @Schema(description = "所属资源 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String resourceId,
        @NotBlank @Schema(description = "资源编码", requiredMode = Schema.RequiredMode.REQUIRED)
        String resourceCode,
        @NotBlank @Schema(description = "资源名称", requiredMode = Schema.RequiredMode.REQUIRED)
        String resourceName,
        @NotNull @Schema(description = "资源允许的范围种类", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid ScopeKind> scopeCapabilities,
        @NotNull @Schema(description = "操作状态", requiredMode = Schema.RequiredMode.REQUIRED)
        ConfigurationStatus status) {

    /**
     * 复制范围能力列表，避免外部修改改变已经计算的解析结果。
     */
    public ActionLookupRecord {
        if (scopeCapabilities != null) {
            scopeCapabilities = Collections.unmodifiableList(new ArrayList<>(scopeCapabilities));
        }
    }
}
