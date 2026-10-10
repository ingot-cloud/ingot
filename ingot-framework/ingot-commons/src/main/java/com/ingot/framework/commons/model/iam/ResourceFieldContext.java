package com.ingot.framework.commons.model.iam;

import java.util.Map;

/**
 * <p>通用字段交互上下文，按精确操作和逻辑字段索引，不按当前页样本求值。</p>
 * @param fieldVisibility 用于列布局的潜在可见性
 * @param fieldOperations 按精确操作索引的全局能力
 * @param masks 当前资源脱敏规则
 * @author jy
 * @since 1.0.0
 */
public record ResourceFieldContext(Map<String, FieldVisibility> fieldVisibility,
        Map<String, Map<String, FieldOperations>> fieldOperations, Map<String, MaskSpec> masks) {
    /** 防御复制上下文。 */
    public ResourceFieldContext {
        fieldVisibility = Map.copyOf(fieldVisibility);
        fieldOperations = fieldOperations.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                Map.Entry::getKey, entry -> Map.copyOf(entry.getValue())));
        masks = Map.copyOf(masks);
    }
}
