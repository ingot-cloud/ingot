package com.ingot.framework.commons.model.iam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>
 * 角色最新已发布版本的一条绑定权限，带目录展示内容，供详情权限 Tab 直接渲染。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 * @param actionId 操作 ID
 * @param actionCode 操作码；目录缺失时为空
 * @param actionName 操作名称；目录缺失时为空
 * @param applicationId 所属应用 ID；目录缺失时为空
 * @param applicationCode 应用编码；目录缺失时为空
 * @param applicationName 应用名称；目录缺失时为空
 * @param resourceId 所属资源 ID；目录缺失时为空
 * @param resourceCode 资源编码；目录缺失时为空
 * @param resourceName 资源名称；目录缺失时为空
 * @param scopes 此操作的范围项
 * @param scopeCapabilities 资源允许的范围种类；目录缺失时为空集合
 * @param status 操作启停；目录缺失时省略
 * @param fieldCapabilities 可信资源字段能力
 * @param fieldDefaults 安全默认值
 * @param fieldPermissions 固定版本字段快照；未声明字段不授予权限
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "角色当前绑定权限，带操作、应用与资源展示内容")
public record RoleGrantRecord(
        @NotBlank @Schema(description = "操作 ID", requiredMode = Schema.RequiredMode.REQUIRED) String actionId,
        @Schema(description = "操作码；目录缺失时为空") String actionCode, @Schema(description = "操作名称；目录缺失时为空") String actionName,
        @Schema(description = "所属应用 ID；目录缺失时为空") String applicationId,
        @Schema(description = "应用编码；目录缺失时为空") String applicationCode,
        @Schema(description = "应用名称；目录缺失时为空") String applicationName,
        @Schema(description = "所属资源 ID；目录缺失时为空") String resourceId,
        @Schema(description = "资源编码；目录缺失时为空") String resourceCode,
        @Schema(description = "资源名称；目录缺失时为空") String resourceName,
        @NotNull @Schema(description = "按并集合成的范围项",
                requiredMode = Schema.RequiredMode.REQUIRED) List<@NotNull @Valid ScopeExpression> scopes,
        @NotNull @Schema(description = "资源允许的范围种类",
                requiredMode = Schema.RequiredMode.REQUIRED) List<@NotNull @Valid ScopeKind> scopeCapabilities,
        @Schema(description = "操作启停；目录缺失时省略") ConfigurationStatus status,
        @Schema(description = "资源字段能力") List<FieldCapability> fieldCapabilities,
        @Schema(description = "资源安全默认值") Map<String, FieldAccess> fieldDefaults,
        @Schema(description = "本版本资源字段快照；未声明字段不授予权限") ResourceFieldDefinition fieldPermissions) {

    /** 无字段能力资源的绑定记录。 */
    public RoleGrantRecord(String actionId, String actionCode, String actionName, String applicationId,
            String applicationCode, String applicationName, String resourceId, String resourceCode, String resourceName,
            List<ScopeExpression> scopes, List<ScopeKind> scopeCapabilities, ConfigurationStatus status) {
        this(actionId, actionCode, actionName, applicationId, applicationCode, applicationName, resourceId,
                resourceCode, resourceName, scopes, scopeCapabilities, status, List.of(), Map.of(), ResourceFieldDefinition.EMPTY);
    }

    /**
     * 复制集合，避免外部修改已经计算的详情结果。
     */
    public RoleGrantRecord {
        fieldCapabilities = List.copyOf(fieldCapabilities);
        fieldDefaults = Map.copyOf(fieldDefaults);
        fieldPermissions = fieldPermissions == null ? ResourceFieldDefinition.EMPTY : fieldPermissions;
        if (scopes != null) {
            scopes = Collections.unmodifiableList(new ArrayList<>(scopes));
        }
        if (scopeCapabilities != null) {
            scopeCapabilities = Collections.unmodifiableList(new ArrayList<>(scopeCapabilities));
        }
    }
}
