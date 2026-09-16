package com.ingot.framework.commons.model.iam;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * <p>描述单个操作的一项范围规则，参数绑定和资源能力由授权服务进一步校验。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param kind 范围种类，必填
 * @param parameterKey 指定管理部门或对象集合的命名参数，其余范围不绑定参数
 * @param includeDescendants 部门范围是否包含下级；非部门范围不使用此项
 */
@Schema(description = "操作范围表达式；资源能力、参数及归属需由服务端验证")
public record ScopeExpression(
        @NotNull @Schema(description = "范围种类", requiredMode = Schema.RequiredMode.REQUIRED)
        ScopeKind kind,
        @Schema(description = "管理部门或对象集合的命名参数")
        String parameterKey,
        @Schema(description = "部门范围是否包含下级")
        Boolean includeDescendants) {

    /**
     * 校验参数与范围种类的组合；具体资源是否支持该范围仍由服务端验证。
     *
     * @return 范围与参数组合是否有效
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    @jakarta.validation.constraints.AssertTrue(message = "范围种类与参数不匹配")
    @Schema(hidden = true)
    public boolean isStructurallyValid() {
        if (kind == null) {
            return true;
        }
        return switch (kind) {
            case ALL, SELF -> parameterKey == null && includeDescendants == null;
            case MEMBER_DEPARTMENTS -> parameterKey == null;
            case MANAGED_DEPARTMENTS -> parameterKey != null && !parameterKey.isBlank();
            case OBJECT_SET -> parameterKey != null && !parameterKey.isBlank() && includeDescendants == null;
        };
    }
}
