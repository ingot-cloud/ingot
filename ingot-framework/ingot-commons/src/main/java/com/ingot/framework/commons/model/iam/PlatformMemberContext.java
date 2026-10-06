package com.ingot.framework.commons.model.iam;

import java.util.Map;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * <p>提供平台成员列布局与新对象创建字段上下文，不替代逐对象鉴权。</p>
 *
 * @param listFieldVisibility 有效读策略中潜在可见的字段，仅用于列设置；每行仍使用自己的字段结果
 * @param createFieldAccess 创建新成员时的字段权限，不用于已有成员编辑
 * @param canSearchDisplayName 整份读策略是否允许按显示名原值搜索
 * @author jy
 * @since 1.0.0
 */
public record PlatformMemberContext(
        @NotNull @Schema(description = "仅用于列布局的整份有效读策略可见性概览") Map<String, FieldVisibility> listFieldVisibility,
        @NotNull @Schema(description = "创建新成员时的精确字段权限") Map<String, FieldAccess> createFieldAccess,
        @Schema(description = "允许按显示名原值搜索", requiredMode = Schema.RequiredMode.REQUIRED) boolean canSearchDisplayName) {

    /**
     * 固化展示上下文，防止调用方修改授权结果。
     */
    public PlatformMemberContext {
        listFieldVisibility = Map.copyOf(listFieldVisibility);
        createFieldAccess = Map.copyOf(createFieldAccess);
    }
}
