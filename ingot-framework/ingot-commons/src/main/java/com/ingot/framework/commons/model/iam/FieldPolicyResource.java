package com.ingot.framework.commons.model.iam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ingot.framework.commons.model.iam.extension.ResourceKey;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>租户可配置字段策略的一份已接入资源。</p>
 *
 * @param resourceId 资源 ID
 * @param resource 完整资源键
 * @param name 资源名称
 * @param fields 目录字段能力
 * @param actions 已绑定字段读写或筛选的动作
 * @author jy
 * @since 1.0.0
 */
@Schema(description = "租户可配置字段策略的一份已接入资源")
public record FieldPolicyResource(
        @NotBlank @Schema(description = "资源 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String resourceId,
        @NotNull @Valid @Schema(description = "完整资源键", requiredMode = Schema.RequiredMode.REQUIRED)
        ResourceKey resource,
        @NotBlank @Schema(description = "资源名称", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @NotNull @Schema(description = "目录字段能力", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid FieldCapability> fields,
        @NotNull @Schema(description = "已绑定字段读写或筛选的动作", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid FieldPolicyAction> actions) {

    /**
     * 复制输出集合。
     */
    public FieldPolicyResource {
        if (fields != null) {
            fields = Collections.unmodifiableList(new ArrayList<>(fields));
        }
        if (actions != null) {
            actions = Collections.unmodifiableList(new ArrayList<>(actions));
        }
    }
}
