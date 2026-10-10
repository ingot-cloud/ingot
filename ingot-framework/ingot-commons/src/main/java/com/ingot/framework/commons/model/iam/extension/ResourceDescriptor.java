package com.ingot.framework.commons.model.iam.extension;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.ingot.framework.commons.model.iam.FieldAccess;
import com.ingot.framework.commons.model.iam.FieldCapability;
import com.ingot.framework.commons.model.iam.FieldVisibility;
import com.ingot.framework.commons.model.iam.ScopeKind;

/**
 * <p>服务器可信资源描述，目录只能收窄其能力。</p>
 *
 * @param key 完整资源键
 * @param actions 后端接入的操作
 * @param scopeCapabilities 支持的范围
 * @param fields 实际执行的字段能力
 * @param defaults 每个字段的默认策略
 * @param readAction 披露诊断对象所需精确操作
 * @param hierarchical 候选是否为树
 * @author jy
 * @since 1.0.0
 */
public record ResourceDescriptor(@NotNull @Valid ResourceKey key, @NotNull List<@Valid ActionDescriptor> actions,
        @NotNull List<ScopeKind> scopeCapabilities, @NotNull List<@Valid FieldCapability> fields,
        @NotNull Map<String, FieldAccess> defaults, @NotBlank String readAction, boolean hierarchical) {
    /**
     * 防御复制；默认值必须覆盖已登记字段。
     */
    public ResourceDescriptor {
        actions = List.copyOf(actions);
        scopeCapabilities = List.copyOf(scopeCapabilities);
        fields = List.copyOf(fields);
        defaults = Map.copyOf(defaults);
        if (actions.stream().map(ActionDescriptor::code).distinct().count() != actions.size()
                || actions.stream().noneMatch(action -> action.code().equals(readAction))) {
            throw new IllegalArgumentException("注册操作必须唯一且包含资源读取操作");
        }
        if (fields.stream().map(FieldCapability::key).distinct().count() != fields.size()
                || !defaults.keySet().equals(new HashSet<>(fields.stream().map(FieldCapability::key).toList()))) {
            throw new IllegalArgumentException("每个注册字段必须声明默认策略");
        }
        for (var field : fields) {
            var value = defaults.get(field.key());
            if (!field.visibilities().contains(value.visibility())
                    || value.editable() && (!field.editable() || value.visibility() == FieldVisibility.HIDDEN)) {
                throw new IllegalArgumentException("字段默认策略超出后端注册能力");
            }
        }
    }
}
