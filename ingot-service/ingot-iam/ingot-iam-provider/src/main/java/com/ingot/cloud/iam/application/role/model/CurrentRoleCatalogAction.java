package com.ingot.cloud.iam.application.role.model;

import com.ingot.cloud.iam.domain.role.CurrentRoleParameter;
import com.ingot.framework.commons.model.iam.FieldAccess;
import com.ingot.framework.commons.model.iam.FieldCapability;
import com.ingot.framework.commons.model.iam.ScopeKind;

import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * <p>返回操作所属资源、真实范围能力、字段能力与稳定参数。</p>
 * @author jy
 * @since 1.0.0
 * @param actionId 精确操作ID
 * @param actionCode 精确操作码
 * @param actionName 操作名称
 * @param applicationId 所属应用ID
 * @param applicationName 应用名称
 * @param resourceId 实际资源ID
 * @param resourceName 资源名称
 * @param enabled 目录及操作是否启用
 * @param scopeCapabilities 资源真实范围能力
 * @param parameterDefinitions 稳定资源参数定义
 * @param fieldCapabilities 资源已声明的字段能力；通讯录资源为空
 * @param fieldDefaults 资源字段默认策略，键与字段能力一致
 */
@Schema(description = "返回操作所属资源、真实范围能力、字段能力与稳定参数")
public record CurrentRoleCatalogAction(
        String actionId,
        String actionCode,
        String actionName,
        String applicationId,
        String applicationName,
        String resourceId,
        String resourceName,
        boolean enabled,
        List<ScopeKind> scopeCapabilities,
        List<CurrentRoleParameter> parameterDefinitions,
        List<FieldCapability> fieldCapabilities,
        Map<String, FieldAccess> fieldDefaults) {

    /**
     * 兼容尚未返回字段能力的目录构造。
     * @param actionId 精确操作ID
     * @param actionCode 精确操作码
     * @param actionName 操作名称
     * @param applicationId 所属应用ID
     * @param applicationName 应用名称
     * @param resourceId 实际资源ID
     * @param resourceName 资源名称
     * @param enabled 目录及操作是否启用
     * @param scopeCapabilities 资源真实范围能力
     * @param parameterDefinitions 稳定资源参数定义
     */
    public CurrentRoleCatalogAction(String actionId, String actionCode, String actionName, String applicationId,
            String applicationName, String resourceId, String resourceName, boolean enabled,
            List<ScopeKind> scopeCapabilities, List<CurrentRoleParameter> parameterDefinitions) {
        this(actionId, actionCode, actionName, applicationId, applicationName, resourceId, resourceName, enabled,
                scopeCapabilities, parameterDefinitions, List.of(), Map.of());
    }

    /** 固定授权集合，避免调用方在验证后修改内容。 */
    public CurrentRoleCatalogAction {
        if (scopeCapabilities != null) scopeCapabilities = List.copyOf(scopeCapabilities);
        if (parameterDefinitions != null) parameterDefinitions = List.copyOf(parameterDefinitions);
        if (fieldCapabilities != null) fieldCapabilities = List.copyOf(fieldCapabilities);
        fieldDefaults = fieldDefaults == null ? Map.of() : Map.copyOf(fieldDefaults);
    }
}
