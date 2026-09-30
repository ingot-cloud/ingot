package com.ingot.framework.commons.model.iam;

/**
 * <p>操作的应用与资源元数据，仅披露配置所需内容。</p>
 * @param id 候选实际标识
 * @param name 可披露名称
 * @param applicationId 操作所属应用标识
 * @param applicationName 应用名称
 * @param resourceId 操作所属资源标识
 * @param resourceName 资源名称
 * @param code 完整精确操作编码
 * @param scopeCapabilities 资源允许使用的范围种类
 * @author jy
 * @since 1.0.0
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record AuthorizationActionOption(@jakarta.validation.constraints.NotBlank String id, @jakarta.validation.constraints.NotBlank String name, String applicationId, String applicationName, String resourceId, String resourceName, String code, @jakarta.validation.constraints.NotNull java.util.List<ScopeKind> scopeCapabilities) {
}
