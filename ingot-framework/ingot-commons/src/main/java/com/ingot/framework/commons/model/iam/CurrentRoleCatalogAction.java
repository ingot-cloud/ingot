package com.ingot.framework.commons.model.iam;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * <p>返回操作所属资源、真实范围能力与稳定参数。</p>
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
 */
@Schema(description = "返回操作所属资源、真实范围能力与稳定参数")
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
        List<CurrentRoleParameter> parameterDefinitions) {

    /** 固定授权集合，避免调用方在验证后修改内容。 */
    public CurrentRoleCatalogAction {
        if (scopeCapabilities != null) scopeCapabilities = List.copyOf(scopeCapabilities);
        if (parameterDefinitions != null) parameterDefinitions = List.copyOf(parameterDefinitions);
    }
}
